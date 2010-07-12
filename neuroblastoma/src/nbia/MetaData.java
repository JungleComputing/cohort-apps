package nbia;

import java.io.Serializable;

public class MetaData implements Serializable {
    private static final long serialVersionUID = 6645005666875524763L;

    public final String ID;
    
    public final long originalOffsetW;
    public final long originalOffsetH;
    
    public final long originalSizeW;
    public final long originalSizeH;
    
    public final int scale;

    public MetaData(final String id, 
            final long originalOffsetW, final long originalOffsetH, 
            final long originalSizeW, final long originalSizeH, 
            final int scale) {
      
        super();
        ID = id;
        this.originalOffsetW = originalOffsetW;
        this.originalOffsetH = originalOffsetH;
        this.originalSizeW = originalSizeW;
        this.originalSizeH = originalSizeH;
        this.scale = scale;
    }

    // Generated
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((ID == null) ? 0 : ID.hashCode());
        result = PRIME * result + (int) (originalOffsetH ^ (originalOffsetH >>> 32));
        result = PRIME * result + (int) (originalOffsetW ^ (originalOffsetW >>> 32));
        result = PRIME * result + (int) (originalSizeH ^ (originalSizeH >>> 32));
        result = PRIME * result + (int) (originalSizeW ^ (originalSizeW >>> 32));
        return result;
    }

    // Generated
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final MetaData other = (MetaData) obj;
        if (ID == null) {
            if (other.ID != null)
                return false;
        } else if (!ID.equals(other.ID))
            return false;
        if (originalOffsetH != other.originalOffsetH)
            return false;
        if (originalOffsetW != other.originalOffsetW)
            return false;
        if (originalSizeH != other.originalSizeH)
            return false;
        if (originalSizeW != other.originalSizeW)
            return false;
        return true;
    }
    
}
