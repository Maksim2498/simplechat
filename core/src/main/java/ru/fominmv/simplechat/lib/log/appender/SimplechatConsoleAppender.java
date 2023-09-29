package ru.fominmv.simplechat.core.log.appender;


import java.io.Serializable;

import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;

import ru.fominmv.simplechat.core.cli.Console;


@Plugin(
    name        = "SimplechatConsoleAppender",
    category    = Core.CATEGORY_NAME,
    elementType = Appender.ELEMENT_TYPE
)
public class SimplechatConsoleAppender extends AbstractAppender {
    @PluginFactory
    public static SimplechatConsoleAppender create(
        @Required
        @PluginAttribute(value = "name")
        String name,

        @PluginElement("Filter")
        Filter filter,

        @PluginElement("Layout")
        Layout<? extends Serializable> layout
    ) {
        if (layout == null)
            layout = PatternLayout.createDefaultLayout();

        return new SimplechatConsoleAppender(name, filter, layout);
    }

    protected SimplechatConsoleAppender(
        String                         name,
        Filter                         filter,
        Layout<? extends Serializable> layout
    ) {
        super(name, filter, layout, true, new Property[0]);
    }

    @Override
    public void append(LogEvent event) {
        final var bytes  = getLayout().toByteArray(event);
        final var string = new String(bytes);

        Console.instance().print(string);
    }
}