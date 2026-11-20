package dev.seedatlas.cli;

import dev.seedatlas.core.validation.ValidationResult;

import java.io.PrintWriter;

final class CliSupport {

    private CliSupport() {
    }

    static void printValidation(PrintWriter out, PrintWriter err, ValidationResult result) {
        for (String warning : result.warnings()) {
            out.println("warning: " + warning);
        }
        for (String error : result.errors()) {
            err.println("error: " + error);
        }
        if (result.valid()) {
            out.println("OK: bundle is valid.");
        } else {
            err.println("INVALID: " + result.errors().size() + " error(s), "
                    + result.warnings().size() + " warning(s).");
        }
    }
}
