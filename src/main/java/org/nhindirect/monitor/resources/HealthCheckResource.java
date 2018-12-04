/* 
Copyright (c) 2010, NHIN Direct Project
All rights reserved.

Authors:
   Greg Meyer      gm2552@cerner.com
 
Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer 
in the documentation and/or other materials provided with the distribution.  Neither the name of the The NHIN Direct Project (nhindirect.org). 
nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS 
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE 
GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF 
THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.nhindirect.monitor.resources;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
/**
 * Resource to detect if the web application is running.
 * @author Greg Meyer
 * @since 1.0
 */
@RestController
@RequestMapping("health")
public class HealthCheckResource 
{
	/**
	 * Cache definition for no caching of responses.
	 */
	protected static final CacheControl noCache;
	
	static
	{
		noCache = CacheControl.noCache();
	}

	/**
	 * Checks to ensure the web application is present.
	 * @return HTTP status 200 with a canned HTML representation.
	 */
    @SuppressWarnings("deprecation")
	@GetMapping(produces = MediaType.TEXT_HTML_VALUE)
	public Mono<String> healthCheck() throws IOException
	{
		// very simple health check to validate the web application is running
		// just return a hard coded HTML resource (HTTP 200) if we're here
    	String respStr = "";
    	
    	final InputStream str = getClass().getResourceAsStream("/html/healthResponse.html");
    	
    	try
    	{
    		respStr = IOUtils.toString(str);
    	
    		return Mono.just(respStr);
    	}
    	finally
    	{
    		IOUtils.closeQuietly(str);
    	}
	}
}
