/*
 * This file is made available under the terms of the LGPL licence.
 * This licence can be retreived from http://www.gnu.org/copyleft/lesser.html.
 * The source remains the property of the YAWL Foundation.  The YAWL Foundation is a collaboration of
 * individuals and organisations who are commited to improving workflow technology.
 *
 */

package au.edu.qut.yawl.persistence.dao;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.jdom.JDOMException;

import com.nexusbpm.editor.tree.DatasourceRoot;

import au.edu.qut.yawl.elements.YSpecification;
import au.edu.qut.yawl.exceptions.YSchemaBuildingException;
import au.edu.qut.yawl.exceptions.YSyntaxException;
import au.edu.qut.yawl.unmarshal.YMarshal;



public class SpecificationFileDAO implements SpecificationDAO{

	public File root;
	
	public boolean delete(YSpecification t) {
		new File(t.getID()).delete();
		return true;
	}

	public SpecificationFileDAO() {
		root = new File(".");
	}
	public SpecificationFileDAO(File root) {
		this.root = root;
	}

	public YSpecification retrieve(Object o) {
		YSpecification retval = null;
            if (o == null) {
				return null;
			}
            try {
				List l = YMarshal.unmarshalSpecifications(o.toString());
				if (l != null && l.size() == 1) {
					retval = (YSpecification) l.get(0);
					retval.setID(new File(o.toString()).toURI().toString());
				}
            } catch (YSyntaxException e) {
				e.printStackTrace();
			} catch (YSchemaBuildingException e) {
				e.printStackTrace();
			} catch (JDOMException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
	            return retval;
	}

	public int save(YSpecification m) {
        File f = new File(m.getID());
        try {
        	FileWriter os = new FileWriter(f);
        	os.write(YMarshal.marshal(m));
        	os.flush();
        	os.close();
        } catch(Exception e) {e.printStackTrace();}
        return 0;
    }

    public Serializable getKey(YSpecification m) {
        return m.getID();
    }

	public List getChildren(Object file) {
		List retval = new ArrayList();
		if (file instanceof String || file instanceof DatasourceRoot) {
			file = file.toString();
			File f = new File((String) file);
			if (f.isFile() && f.getName().endsWith(".xml")) {
				YSpecification spec = retrieve(f.getAbsolutePath());
				retval.add(spec);
			} else {
				File[] files = (new File((String) file)).listFiles();
				if (files != null) {
					for (File aFile : files) {
						retval.add(aFile.getAbsolutePath());
					}
				}
			}
		}
		return retval;
	}
}
