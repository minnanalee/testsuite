module com.github.nhojpatrick.config.core {
    exports com.github.nhojpatrick.config.core;
    exports com.github.nhojpatrick.config.core.internal
            to com.github.nhojpatrick.config.core.test;
    exports com.github.nhojpatrick.config.core.files;
    exports com.github.nhojpatrick.config.core.files.internal
            to com.github.nhojpatrick.config.core.test;
    exports com.github.nhojpatrick.config.core.properties;
    exports com.github.nhojpatrick.config.core.properties.internal
            to com.github.nhojpatrick.config.core.test;
    requires com.github.spotbugs.annotations;
    requires org.apache.commons.collections4;
    requires org.apache.commons.lang3;
    requires org.slf4j;
    requires transitive org.apache.commons.configuration2;
}