/*
 * Copyright 2010-2018 Norwegian Agency for Public Management and eGovernment (Difi)
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they
 * will be approved by the European Commission - subsequent
 * versions of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 *
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/community/eupl/og_page/eupl
 *
 * Unless required by applicable law or agreed to in
 * writing, software distributed under the Licence is
 * distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied.
 * See the Licence for the specific language governing
 * permissions and limitations under the Licence.
 */

package no.difi.oxalis.inbound;

import com.github.kristofa.brave.Brave;
import com.github.kristofa.brave.servlet.BraveServletFilter;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.servlet.ServletModule;
import no.difi.oxalis.api.inbound.InboundService;
import no.difi.oxalis.inbound.servlet.HomeServlet;
import no.difi.oxalis.inbound.servlet.StatusServlet;

/**
 * @author erlend
 */
public class OxalisInboundModule extends ServletModule {

    @Override
    protected void configureServlets() {
        // Enable Zipkin tracing
        filterRegex("/*").through(BraveServletFilter.class);

        serve("/").with(HomeServlet.class);
        serve("/status").with(StatusServlet.class);

        bind(InboundService.class).to(DefaultInboundService.class);
    }

    @Provides
    @Singleton
    protected BraveServletFilter getBraveServletFilter(Brave brave) {
        return BraveServletFilter.create(brave);
    }
}
