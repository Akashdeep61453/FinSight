package com.financeinsight.parser;

import com.financeinsight.common.exception.StatementNotFoundException;
import com.financeinsight.statement.Statement;
import com.financeinsight.statement.StatementRepository;
import com.financeinsight.storage.StatementStorage;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PdfTextExtractor {

    private final StatementRepository statementRepository;
    private final StatementStorage statementStorage;

    public PdfTextExtractor(StatementRepository statementRepository, StatementStorage statementStorage) {
        this.statementRepository = statementRepository;
        this.statementStorage = statementStorage;
    }

    public ExtractedPdfText extract(UUID statementId) {
        Statement statement = statementRepository.findById(statementId)
                .orElseThrow(StatementNotFoundException::new);

        byte[] pdfBytes;
        try (InputStream input = statementStorage.open(statement.getStorageKey())) {
            pdfBytes = input.readAllBytes();
        } catch (IOException exception) {
            throw new PdfExtractionException(PdfExtractionFailure.STORAGE_UNAVAILABLE,
                    "The uploaded statement file could not be accessed", exception);
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            if (document.isEncrypted() && !document.getCurrentAccessPermission().canExtractContent()) {
                throw new PdfExtractionException(PdfExtractionFailure.PASSWORD_PROTECTED,
                        "Text extraction is not permitted for this PDF");
            }
            return extractPages(document);
        } catch (InvalidPasswordException exception) {
            throw new PdfExtractionException(PdfExtractionFailure.PASSWORD_PROTECTED,
                    "The PDF is password-protected", exception);
        } catch (PdfExtractionException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new PdfExtractionException(PdfExtractionFailure.INVALID_PDF,
                    "The stored file could not be read as a PDF", exception);
        }
    }

    private ExtractedPdfText extractPages(PDDocument document) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        List<String> pages = new ArrayList<>(document.getNumberOfPages());
        boolean hasExtractableText = false;
        for (int pageNumber = 1; pageNumber <= document.getNumberOfPages(); pageNumber++) {
            stripper.setStartPage(pageNumber);
            stripper.setEndPage(pageNumber);
            String pageText = stripper.getText(document).strip();
            pages.add(pageText);
            if (!pageText.isBlank()) {
                hasExtractableText = true;
            }
        }
        if (!hasExtractableText) {
            throw new PdfExtractionException(PdfExtractionFailure.NO_EXTRACTABLE_TEXT,
                    "The PDF contains no extractable text; it may be a scanned image statement");
        }
        return new ExtractedPdfText(pages);
    }
}
