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
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.ServerContainer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import se.sics.emul8.radiomedium.RadioListener;
import se.sics.emul8.radiomedium.RadioMedium;
import se.sics.emul8.radiomedium.RadioPacket;
import se.sics.emul8.radiomedium.Simulator;
import se.sics.emul8.radiomedium.net.ClientConnection;
import se.sics.jipv6.analyzer.ExampleAnalyzer;
import se.sics.jipv6.analyzer.JShark;
import se.sics.jipv6.pcap.CapturedPacket;
import se.sics.jipv6.server.SnifferServer;
import se.sics.jipv6.server.SnifferSocket;

public class WebServer extends AbstractHandler {

    Server server;
    Simulator simulator;
    JShark jshark;
    SnifferServer sniffer;

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
        sniffer = SnifferServer.getDefault();
        jshark = new JShark(new ExampleAnalyzer(), sniffer.getOutput());
        jshark.setStorePackets(true);
        sniffer.setSniffer(jshark);
        simulator.addRadioListener(new RadioListener() {
            @Override
            public void packetTransmission(RadioPacket packet) {
                byte[] data = packet.getPacketDataAsBytes();
                CapturedPacket capPacket = new CapturedPacket(simulator.getSimulationStartTimeMillis() + packet.getStartTime() / 1000, Arrays.copyOfRange(data, 1, data.length));
                System.out.println("Packet received and sent on to jshark len:" + (packet.getPacketDataAsBytes().length - 1));
                try {
                    jshark.packetData(capPacket);
                } catch (Exception e) {
                    // TODO: handle exception
                    e.printStackTrace();
                }
            }
        });
    }

    public void startWS() {
        Runnable r = new Runnable() {
            public void run() {
                server = new Server(8080);

                ResourceHandler resourceHandler = new ResourceHandler();
                resourceHandler.setDirectoriesListed(true);
                resourceHandler.setResourceBase("./www");
                ContextHandler resourceContext = new ContextHandler("/www");
                resourceContext.setHandler(resourceHandler);

                ContextHandler rmContext = new ContextHandler("/");
                rmContext.setContextPath("/");
                rmContext.setHandler(WebServer.this);

                ContextHandler contextSniff = new ContextHandler("/sniffer");
                contextSniff.setHandler(sniffer);
                sniffer.setSniffer(jshark);

                ServletContextHandler servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
                servletContext.setContextPath("/");

                ContextHandlerCollection contexts = new ContextHandlerCollection();
                contexts.setHandlers(new Handler[] { resourceContext, rmContext, contextSniff, servletContext });

                server.setHandler(contexts);
                try {
                    // Initialize javax.websocket layer
                    ServerContainer wscontainer = WebSocketServerContainerInitializer.configureContext(servletContext);

                    // Add WebSocket endpoint to javax.websocket layer
                    wscontainer.addEndpoint(SnifferSocket.class);

                    System.out.println("Starting jetty web server at 8080");
                    server.start();
                    server.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("web server error");
                    e.printStackTrace();
                } catch (Exception e) {
                    System.err.println("web server error");
                    e.printStackTrace();
                }
            }
        };
        new Thread(r).start();
    }

    public void stopWS() {
        try {
            server.stop();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

}
