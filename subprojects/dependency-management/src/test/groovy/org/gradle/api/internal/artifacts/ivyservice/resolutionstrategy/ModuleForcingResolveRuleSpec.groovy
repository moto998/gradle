/*
 * Copyright 2012 the original author or authors.
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

package org.gradle.api.internal.artifacts.ivyservice.resolutionstrategy

import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.internal.artifacts.DependencySubstitutionInternal
import org.gradle.api.internal.artifacts.ImmutableModuleIdentifierFactory
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.result.VersionSelectionReasons
import org.gradle.internal.component.external.model.DefaultModuleComponentSelector
import spock.lang.Specification

import static org.gradle.api.internal.artifacts.DefaultModuleVersionSelector.newSelector

class ModuleForcingResolveRuleSpec extends Specification {

    final ImmutableModuleIdentifierFactory moduleIdentifierFactory = Mock() {
        module(_, _) >> { args ->
            DefaultModuleIdentifier.newId(*args)
        }
    }

    def "forces modules"() {
        given:
        def details = Mock(DependencySubstitutionInternal)

        when:
        new ModuleForcingResolveRule([
            newSelector("org", "module1", "1.0"),
            newSelector("org", "module2", "2.0"),
            //exotic module with colon in the name
            newSelector("org", "module:with:colon", "3.0"),
            newSelector("org:with:colon", "module2", "4.0")
        ], moduleIdentifierFactory).execute(details)

        then:
        _ * details.requested >> DefaultModuleComponentSelector.newSelector(requested)
        _ * details.getOldRequested() >> requested
        1 * details.useTarget(DefaultModuleComponentSelector.newSelector(requested.group, requested.name, forcedVersion), VersionSelectionReasons.FORCED)
        0 * details._

        where:
        requested                                        | forcedVersion
        newSelector("org",  "module2", "0.9")            | "2.0"
        newSelector("org",  "module2", "2.1")            | "2.0"
        newSelector("org",  "module:with:colon", "2.0")  | "3.0"
        newSelector("org:with:colon",  "module2", "5.0") | "4.0"
    }

    def "does not force modules if they dont match"() {
        given:
        def details = Mock(DependencySubstitutionInternal)

        when:
        new ModuleForcingResolveRule([
            newSelector("org", "module1", "1.0"),
            newSelector("org", "module2", "2.0"),
            newSelector("org", "module:with:colon", "3.0"),
            newSelector("org:with:colon", "module2", "4.0")
        ], moduleIdentifierFactory).execute(details)

        then:
        _ * details.getOldRequested() >> requested
        0 * details._

        where:
        requested << [
            newSelector("orgX", "module2", "0.9"),
            newSelector("org",  "moduleX", "2.9"),
            newSelector("orgX",  "module:with:colon", "2.9"),
            newSelector("org:with:colon",  "moduleX", "2.9"),
            newSelector("org:with",  "colon:module2", "2.9"),
            newSelector("org",  "with:colon:module2", "2.9"),
        ]
    }

    def "does not force anything when input empty"() {
        def details = Mock(DependencySubstitutionInternal)

        when:
        new ModuleForcingResolveRule([], moduleIdentifierFactory).execute(details)

        then:
        0 * details._
    }
}
