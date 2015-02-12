package se.sics.sim.core;

import java.util.Hashtable;

/**
 * Describe class Setup here.
 *
 *
 * Created: Tue Apr 24 10:22:57 2007
 *
 * @author <a href="mailto:joakime@GRAYLING"></a>
 * @version 1.0
 */
public interface Setup {

    public Hashtable<Object,Object> getProperties();

    public Object getProperty(Object name);

    public Object getProperty(Object name, Object defaultValue);

    public Object setProperty(Object name, Object value);

    public void setup(Simulator sim);

    public void finish(Simulator sim);

}
