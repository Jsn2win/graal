/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.svm.core.jdk;

import com.oracle.svm.core.option.SubstrateOptionsParser;
import org.graalvm.collections.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.spi.LocaleServiceProvider;

//Checkstyle: stop
import sun.util.locale.provider.LocaleProviderAdapter;
//Checkstyle: resume

public class OptimizedLocalizationSupport extends LocalizationSupport {
    final Map<Pair<Class<? extends LocaleServiceProvider>, Locale>, LocaleProviderAdapter> adaptersByClass = new HashMap<>();
    final Map<LocaleProviderAdapter.Type, LocaleProviderAdapter> adaptersByType = new HashMap<>();
    final Map<Class<? extends LocaleServiceProvider>, Object> providerPools = new HashMap<>();
    final Map<Pair<String, Locale>, ResourceBundle> resourceBundles = new HashMap<>();

    private final String includeResourceBundlesOption = SubstrateOptionsParser.commandArgument(LocalizationFeature.Options.IncludeResourceBundles, "");

    public OptimizedLocalizationSupport(Locale defaultLocale, List<Locale> locales) {
        super(defaultLocale, locales);
    }

    final ResourceBundle.Control control = ResourceBundle.Control.getControl(ResourceBundle.Control.FORMAT_DEFAULT);

    /**
     * Get cached resource bundle.
     *
     * @param locale this parameter is not currently used.
     */
    public ResourceBundle getCached(String baseName, Locale locale) throws MissingResourceException {
        // todo optimize the map into a trie-like structure instead instead of linear search?
        for (Locale candidateLocale : control.getCandidateLocales(baseName, locale)) {
            ResourceBundle result = resourceBundles.get(Pair.create(baseName, candidateLocale));
            if (result != null) {
                return result;
            }
        }
        String errorMessage = "Resource bundle not found " + baseName + ", locale + " + locale + ". " +
                        "Register the resource bundle using the option " + includeResourceBundlesOption + baseName + ".";
        for (Pair<String, Locale> pair : resourceBundles.keySet()) {
            System.err.println(pair);
        }
        throw new MissingResourceException(errorMessage, this.getClass().getName(), baseName);

    }
}
