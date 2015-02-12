/*
 * Copyright (c) 2007, SICS AB.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 * 3. The name of the author may not be used to endorse or promote
 *    products derived from this software without specific prior
 *    written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * $Id: AbstractSetup.java,v 1.3 2007/05/31 12:38:17 joakime Exp $
 *
 * -----------------------------------------------------------------
 *
 * AbstractSetup
 *
 * Authors : Joakim Eriksson, Niclas Finne
 * Created : Thu May 24 14:48:29 2007
 * Updated : $Date: 2007/05/31 12:38:17 $
 *           $Revision: 1.3 $
 */
package se.sics.sim.core;

import java.util.Hashtable;
import se.sics.sim.rl.*;

/**
 */
public abstract class AbstractSetup implements Setup {

    private Hashtable<Object,Object> propertyTable;

    public void setup(Simulator sim) {
        if (getProperty("episodeManager") != null) {
            EpisodeManager.setDefault(new EpisodeManager());
        }
    }

    public Hashtable<Object,Object> getProperties() {
        return propertyTable;
    }

    public Object getProperty(Object key) {
        return getProperty(key, null);
    }

    public Object getProperty(Object key, Object defaultValue) {
        if (propertyTable == null) {
            return defaultValue;
        }
        Object v = propertyTable.get(key);
        return v == null ? defaultValue : v;
    }

    public Object setProperty(Object key, Object value) {
        if (propertyTable == null) {
            propertyTable = new Hashtable<Object,Object>();
        }
        return propertyTable.put(key, value);
    }

    public long getPropertyAsLong(Object key, long defaultValue) {
        Object v = getProperty(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Long) {
            return ((Long) v).longValue();
        }
        if (v instanceof Integer) {
            return ((Integer) v).intValue();
        }
        try {
            return Long.parseLong(v.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public double getPropertyAsDouble(Object key, double defaultValue) {
        Object v = getProperty(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Double) {
            return ((Double) v).doubleValue();
        }
        if (v instanceof Long) {
            return ((Long) v).longValue();
        }
        if (v instanceof Integer) {
            return ((Integer) v).intValue();
        }
        try {
            return Double.parseDouble(v.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public int getPropertyAsInt(Object key, int defaultValue) {
        Object v = getProperty(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Integer) {
            return ((Integer) v).intValue();
        }
        try {
            return Integer.parseInt(v.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean getPropertyAsBoolean(Object key, boolean defaultValue) {
        Object v = getProperty(key);
        if (v == null) {
            return defaultValue;
        }
        if (v instanceof Boolean) {
            return ((Boolean) v).booleanValue();
        }
        try {
            return Boolean.parseBoolean(v.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

} // AbstractSetup
