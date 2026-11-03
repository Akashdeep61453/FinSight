package com.financeinsight.statement;

import com.financeinsight.parser.ParserRouter;
import com.financeinsight.parser.PdfExtractionException;
import com.financeinsight.parser.PdfExtractionFailure;
import com.financeinsight.parser.PdfTextExtractor;
import com.financeinsight.parser.UnsupportedStatementFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StatementProcessingService {

    private static final Logger log = LoggerFactory.getLogger(StatementProcessingService.class);

    private final StatementProcessingWriter writer;
    private final PdfTextExtractor textExtractor;
    private final ParserRouter parserRouter;

    public StatementProcessingService(StatementProcessingWriter writer,
                                      PdfTextExtractor textExtractor,
                                      ParserRouter parserRouter) {
        this.writer = writer;
        this.textExtractor = textExtractor;
        this.parserRouter = parserRouter;
    }

    public void process(UUID statementId) {
        writer.start(statementId);
        try {
            var extracted = textExtractor.extract(statementId);
            var parsed = parserRouter.parse(extracted);
            writer.finish(statementId, parsed);
        } catch (UnsupportedStatementFormatException exception) {
            writer.needsReview(statementId, exception.getMessage());
        } catch (PdfExtractionException exception) {
            if (exception.getFailure() == PdfExtractionFailure.NO_EXTRACTABLE_TEXT) {
                writer.needsReview(statementId, exception.getMessage());
            } else {
                writer.failed(statementId, exception.getMessage());
            }
        } catch (RuntimeException exception) {
            log.warn("Statement processing failed for statement {} ({})", statementId,
                    exception.getClass().getSimpleName());
            writer.failed(statementId, "Statement processing failed. Check the PDF and try again.");
        }
    }
}
