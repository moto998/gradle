/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.reporting.dependents;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import org.gradle.api.DefaultTask;
import org.gradle.api.Incubating;
import org.gradle.api.Project;
import org.gradle.api.internal.tasks.options.Option;
import org.gradle.api.reporting.dependents.internal.TextDependentComponentsReportRenderer;
import org.gradle.api.tasks.Console;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.logging.text.StyledTextOutput;
import org.gradle.internal.logging.text.StyledTextOutputFactory;
import org.gradle.model.internal.registry.ModelRegistry;
import org.gradle.platform.base.ComponentSpec;
import org.gradle.platform.base.internal.dependents.DependentBinariesResolver;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

import static org.gradle.api.reporting.dependents.internal.DependentComponentsUtils.getAllComponents;

/**
 * Displays dependent components.
 */
@Incubating
public class DependentComponentsReport extends DefaultTask {

    private boolean detail;
    private List<String> components;

    public boolean isDetail() {
        return detail;
    }

    @Option(option = "all", description = "Show non-buildables dependents and detail.")
    public void setDetail(boolean detail) {
        this.detail = detail;
    }

    /**
     * Returns the components to generate the report for.
     * Defaults to all components of this task's containing project.
     *
     * @return the components.
     */
    @Console
    public List<String> getComponents() {
        return components;
    }

    /**
     * Sets the components to generate the report for.
     *
     * @param components the components.
     */
    @Option(option = "component", description = "Component to generate the report for, repeatable.")
    public void setComponents(List<String> components) {
        this.components = components;
    }

    @Inject
    protected StyledTextOutputFactory getTextOutputFactory() {
        throw new UnsupportedOperationException();
    }

    @Inject
    protected ModelRegistry getModelRegistry() {
        throw new UnsupportedOperationException();
    }

    @TaskAction
    public void report() {
        Project project = getProject();
        ModelRegistry modelRegistry = getModelRegistry();
        DependentBinariesResolver dependentBinariesResolver = modelRegistry.find("dependentBinariesResolver", DependentBinariesResolver.class);

        StyledTextOutput textOutput = getTextOutputFactory().create(DependentComponentsReport.class);
        TextDependentComponentsReportRenderer reportRenderer = new TextDependentComponentsReportRenderer(dependentBinariesResolver, detail);

        reportRenderer.setOutput(textOutput);
        reportRenderer.startProject(project);

        Set<ComponentSpec> allComponents = getAllComponents(modelRegistry);
        reportRenderer.renderComponents(getReportedComponents(allComponents));

        reportRenderer.completeProject(project);
        reportRenderer.complete();
    }

    private Set<ComponentSpec> getReportedComponents(Set<ComponentSpec> allComponents) {
        if (components == null || components.isEmpty()) {
            return allComponents;
        }
        return Sets.filter(allComponents, new ReportedComponentPredicate(components));
    }

    private static class ReportedComponentPredicate implements Predicate<ComponentSpec> {
        private final List<String> reported;

        private ReportedComponentPredicate(List<String> reported) {
            this.reported = reported;
        }

        @Override
        public boolean apply(ComponentSpec component) {
            return reported.contains(component.getName());
        }
    }

}