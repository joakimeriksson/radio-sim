/*
 * Copyright (c) 2015, SICS Swedish ICT.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the Institute nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE INSTITUTE OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 *
 * \author
 *      Joakim Eriksson <joakime@sics.se> & Niclas Finne <nfi@sics.se>
 *
 */

package se.sics.emul8.web;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import se.sics.emul8.radiomedium.RadioMedium;
import se.sics.emul8.radiomedium.Simulator;
import se.sics.emul8.radiomedium.net.ClientConnection;

public class WebServer extends AbstractHandler {

    Server server;    
    Simulator simulator;
    
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html; charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().println("<h1>Radio Medium Simulation</h1>");
        
        if (simulator != null) {
            RadioMedium rm = simulator.getRadioMedium();
            String name = rm.getName();
            response.getWriter().println("Radio medium:" + name + "<br>");
            
            ClientConnection[] clients = simulator.getEmulators();
            if(clients != null) {
                response.getWriter().println("Emulators: <ul>");
                for(int i = 0; i < clients.length; i++) {
                    response.getWriter().println("<li>" + clients[i].getName() + "<br>");
                }
                response.getWriter().println("</ul>");
            } else {
                response.getWriter().println("No emulators connected.<br>");
            }
        } else {
            response.getWriter().println("No simulator set<br>");
        }
        baseRequest.setHandled(true);
    }
    
    public void setSimulator(Simulator simulator) {
        // TODO Auto-generated method stub
        this.simulator = simulator;
    }

    public void startWS() {
        Runnable r = new Runnable() {
            public void run() {
                server = new Server(8080);
                server.setHandler(WebServer.this);
                try {
                    System.out.println("Starting jetty web server at 8080");
                    server.start();
                    server.join();
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
        new Thread(r).start();
    }

    public void stopWS() {
	try {
            server.stop();
	}
	catch(Exception e) {
            e.printStackTrace();
	}
    }
    
    public static void main(String[] args) throws Exception {
        Server server = new Server(8080);
        server.setHandler(new WebServer());
        server.start();
        server.join();
    }

}
