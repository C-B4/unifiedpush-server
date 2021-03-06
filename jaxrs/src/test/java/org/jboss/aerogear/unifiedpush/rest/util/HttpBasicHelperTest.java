/**
 * JBoss, Home of Professional Open Source
 * Copyright Red Hat, Inc., and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.aerogear.unifiedpush.rest.util;

import org.junit.Test;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpBasicHelperTest {

	@Test
	public void extractUsernameAndPasswordFromBasicHeader() {
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		final String encodedUserPassword = Base64.getEncoder()
				.encodeToString("user:password".getBytes(StandardCharsets.UTF_8));
		Mockito.when(request.getHeader("Authorization")).thenReturn("Basic " + encodedUserPassword);

		final String[] credentials = HttpBasicHelper.extractUsernameAndPasswordFromBasicHeader(request);

		assertThat(credentials).isNotNull();
		assertThat(credentials[0]).isEqualTo("user");
		assertThat(credentials[1]).isEqualTo("password");
		assertThat(credentials[0]).isNotEqualTo(" user");
		assertThat(credentials[1]).isNotEqualTo(" password");
	}

	@Test
	public void tryToExtractUsernameAndPasswordFromEmptyHeader() {
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getHeader("Authorization")).thenReturn("");

		final String[] credentials = HttpBasicHelper.extractUsernameAndPasswordFromBasicHeader(request);
		assertThat(credentials).isNotNull();
		assertThat(credentials[0]).isEqualTo("");
		assertThat(credentials[1]).isEqualTo("");
	}

	@Test
	public void tryToExtractUsernameAndPasswordFromNullHeader() {
		final HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getHeader("Authorization")).thenReturn(null);

		final String[] credentials = HttpBasicHelper.extractUsernameAndPasswordFromBasicHeader(request);
		assertThat(credentials).isNotNull();
		assertThat(credentials[0]).isEqualTo("");
		assertThat(credentials[1]).isEqualTo("");
	}

	@Test
	public void tryToDecode() {
		String credentials = HttpBasicHelper.decodeBase64(
				"Znk1S2hSb2lvRk06QVBBOTFiSDJBTE01bmlBMHQ5eGI3Vmp1SzJjbTVyLUYwQ1hxMzFSVE15S190Qk54UFlhUmNIaU5wWUZVdnVrMkdKLTZ5eXlUYnNMS1BBNDA4a0Z3UTg0eDR6SkhxVnlRbjNaQzNFamIwTEl2ZVhHcXpackEtTkJBYWZvZFNPMTBndEVpUDAwR01NcDQ=");
		assertThat(credentials).matches(
				"fy5KhRoioFM:APA91bH2ALM5niA0t9xb7VjuK2cm5r-F0CXq31RTMyK_tBNxPYaRcHiNpYFUvuk2GJ-6yyyTbsLKPA408kFwQ84x4zJHqVyQn3ZC3Ejb0LIveXGqzZrA-NBAafodSO10gtEiP00GMMp4");
	}
}
