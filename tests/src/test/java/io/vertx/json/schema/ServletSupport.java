/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.vertx.json.schema;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Objects;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.component.LifeCycle;

public class ServletSupport {

  public static ServletSupport withDocumentRoot(final String path) {
    try {
      return withDocumentRoot(new File(ServletSupport.class.getResource(path).toURI()));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static ServletSupport withDocumentRoot(final File documentRoot) {
    return new ServletSupport(documentRoot);
  }

  private final File documentRoot;

  public ServletSupport(final File documentRoot) {
    this.documentRoot = Objects.requireNonNull(documentRoot, "documentRoot cannot be null");
  }
  private Server server;
  public void run(final Runnable runnable) {  
    server = new Server(1234);
    ServletHandler handler = new ServletHandler();
    server.setHandler(handler);
    handler.addServletWithMapping(new ServletHolder(new IssueServlet(documentRoot)), "/*");
    server.addLifeCycleListener(new LifeCycle.Listener() {
			
			@Override
			public void lifeCycleStopping(LifeCycle arg0) {
			}
			
			@Override
			public void lifeCycleStopped(LifeCycle arg0) {
				
			}
			
			@Override
			public void lifeCycleStarting(LifeCycle arg0) {
				
			}
			
			@Override
			public void lifeCycleStarted(LifeCycle arg0) {
				runnable.run();
				stopJetty();
			}
			
			@Override
			public void lifeCycleFailure(LifeCycle arg0, Throwable arg1) {
				
			}
		});
    try {
      server.start();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void stopJetty() {
    if (server != null) {
      try {
        server.stop();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    server = null;
  }

}
