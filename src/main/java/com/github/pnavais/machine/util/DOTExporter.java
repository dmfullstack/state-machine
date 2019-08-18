/*
 * Copyright 2019 Pablo Navais
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.github.pnavais.machine.util;

import com.github.pnavais.machine.StateMachine;
import com.github.pnavais.machine.api.exception.FileExportException;
import com.github.pnavais.machine.api.exporter.Exporter;
import com.github.pnavais.machine.api.message.Message;
import com.github.pnavais.machine.model.State;
import lombok.*;
import lombok.extern.java.Log;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * An exporter allowing to translate
 * a given state machine to the GraphViz DOT language
 * (@see https://www.graphviz.org/doc/info/lang.html)
 */
@Log
@Getter
@Setter
@Builder(toBuilder=true)
@AllArgsConstructor
@NoArgsConstructor
public class DOTExporter implements Exporter<String, State, Message, StateMachine> {

    /**
     * The possible Rank direction of the grapth
     */
    public enum RankDir {
        LR("rankdir=\"LR\";"), /* Left to right */
        TB("rankdir=\"TB\";"); /* Top to bottom */

        private String value;

        RankDir(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /** The system's new line character */
    private static final String NL = System.lineSeparator();

    /** The tabulator character */
    private static final char TB = '\t';

    /** The default color for final states */
    public static final String DEFAULT_FINAL_COLOR = "#C2B3FF";

    /** The file system to use when locating paths */
    @Builder.Default
    private FileSystem fileSystem = FileSystems.getDefault();

    /** The direction of the graph */
    @Builder.Default
    private RankDir rankDir = RankDir.LR;

    /** The name of the graph */
    @Builder.Default
    private String graphName = "G";

    /** The color for final states */
    @Builder.Default
    private Color finalStateColor = Color.decode(DEFAULT_FINAL_COLOR);

    /** Use HSB colors in the output format. (Defaults to RGB) */
    private boolean useHSB;

    /**
     * Export the current contents of the state machine
     * to the DOT language.
     *
     * @param stateMachine the state machine to export
     * @return the string representation of the state machine
     * in DOT language.
     */
    @Override
    public String export(@NonNull StateMachine stateMachine) {
        StringBuilder builder = new StringBuilder("digraph ");
        builder.append(getGraphName());
        builder.append(" {");
        builder.append(NL);
        builder.append(TB).append(getRankDir()).append(NL);
        appendNodesDescription(stateMachine, builder);
        appendTransitions(stateMachine, builder);

        return builder.append("}").toString();
    }

    /**
     * Appends the node properties of the state machine.
     *
     * @param stateMachine the state machine
     * @param builder the builder
     */
    private void appendNodesDescription(StateMachine stateMachine, StringBuilder builder) {
        Map<State, Map<Message, State>> transitions = stateMachine.getTransitionsIndex().getTransitionsAsMap();
        for (State s : transitions.keySet()) {
            String prefix = TB + s.getName() + " [";

            if ((s.isFinal() && (!s.hasProperty("color")))) {
                builder.append(prefix).append("style=\"filled\", color=\"").append(toOutputColor(getFinalStateColor())).append("\"");
                prefix = "";
            }

            if (s.hasProperties()) {
                builder.append(prefix);
                prefix = "";
                final String[] finalPrefix = { prefix };
                s.getProperties().keySet().stream().map(k -> k + "=\"" + s.getProperties().get(k) + "\"").forEachOrdered(p -> {
                    builder.append(finalPrefix[0]).append(p);
                    finalPrefix[0] = ", ";
                });
            }

            if (prefix.equals("")) {
                builder.append("];").append(NL);
            }
        }

    }

    /**
     * Appends the node transitions of the state machine.
     *
     * @param stateMachine the state machine
     * @param builder the builder
     */
    private void appendTransitions(StateMachine stateMachine, StringBuilder builder) {
        stateMachine.getAllTransitions().forEach(t ->
                builder.append(TB).append(String.format("%s -> %s [label=\"%s\"];", t.getOrigin().getName(), t.getTarget().getName(), t.getMessage()))
                        .append(NL));
    }

    /**
     * Translates the color to and HSB/RGB string representation
     *
     * @param color the color
     * @return the HSB/RGB string representation
     */
    private String toOutputColor(Color color) {
        return (isUseHSB()) ? ColorTranslator.toHSBColor(color) : ColorTranslator.toRGBColor(color);
    }



    /**
     * Export the current contents of the state machine
     * to the DOT language to the given file path.
     *
     * @param stateMachine the state machine to export
     * @param outputFile the output file path
     */
    public void exportToFile(@NonNull StateMachine stateMachine, @NonNull String outputFile) {
        exportToFile(stateMachine, getFileSystem().getPath(outputFile));
    }

    /**
     * Export the current contents of the state machine
     * to the DOT language to the given file path.
     *
     * @param stateMachine the state machine to export
     * @param outputFile the output file path
     */
    @Override
    public void exportToFile(@NonNull StateMachine stateMachine, @NonNull Path outputFile) {
        try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            writer.write(export(stateMachine));
        } catch (IOException ex) {
            log.throwing(getClass().getSimpleName(), "exportToFile", ex);
            throw new FileExportException("Error exporting output file", ex);
        }
    }

}