package eu.modernmt.processing.builder;

import eu.modernmt.model.UnsupportedLanguageException;
import eu.modernmt.processing.ProcessingException;
import eu.modernmt.processing.TextProcessor;

import java.util.List;
import java.util.Locale;

/**
 * Created by davide on 31/05/16.
 */
class ProcessorGroupBuilder extends AbstractBuilder {

    private final List<ProcessorBuilder> builders;

    ProcessorGroupBuilder(List<ProcessorBuilder> builders) {
        this.builders = builders;
    }

    @Override
    public <P, R> TextProcessor<P, R> create(Locale sourceLanguage, Locale targetLanguage) throws ProcessingException {
        for (ProcessorBuilder builder : builders) {
            if (builder.accept(sourceLanguage, targetLanguage))
                return builder.create(sourceLanguage, targetLanguage);
        }

        throw new UnsupportedLanguageException(sourceLanguage, targetLanguage);
    }

}
