/*
 * Created on Feb 12, 2005
 *
 */
package org.eclipse.ecf.ui.presence;

import java.util.Map;
import java.util.Properties;

public class Presence implements IPresence {
    
    protected Type type;
    protected Mode mode;
    protected int priority;
    protected String status;
    protected Map properties;
    
    public Presence() {
        this(Type.AVAILABLE);
    }
    public Presence(Type type) {
        this(type,"",Mode.AVAILABLE);
    }
    public Presence(Type type, int priority, String status, Mode mode, Map props) {
        this.type = type;
        this.priority = priority;
        this.status = status;
        this.mode = mode;
        this.properties = props;
    }
    public Presence(Type type, int priority, String status, Mode mode) {
        this(type,priority,status,mode,new Properties());
    }
    public Presence(Type type, String status, Mode mode) {
        this(type,-1,status,mode);
    }
    public Mode getMode() {
        return mode;
    }
    public int getPriority() {
        return priority;
    }
    public Map getProperties() {
        return properties;
    }
    public String getStatus() {
        return status;
    }
    public Type getType() {
        return type;
    }
    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
     */
    public Object getAdapter(Class adapter) {
        return null;
    }

}
