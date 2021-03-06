/*
 * Copyright (C) 2008 Rega Institute for Medical Research, KULeuven
 * 
 * See the LICENSE file for terms of use.
 */
package rega.genotype.ui.i18n.resources;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import eu.webtoolkit.jwt.WApplication;
import eu.webtoolkit.jwt.WLocalizedStrings;
import eu.webtoolkit.jwt.WString;
import eu.webtoolkit.jwt.WWebWidget;

public class GenotypeResourceManager extends WLocalizedStrings {
	private Map<String, String> resources = new HashMap<String, String>();
	
	private String commonXml;
	private String organismXml;
	
	private Element common;
	private Element organism;
	
	public GenotypeResourceManager(String commonXml, String organismXml) {
		this.commonXml = commonXml;
		this.organismXml = organismXml;
		
		refresh();
	}
	
	private Element processDoc(String xmlName) {
		SAXBuilder builder = new SAXBuilder();
		Document doc = null;
		String name;
		String value;
		try {
			doc = builder.build(this.getClass().getResourceAsStream(xmlName));
			Element root = doc.getRootElement();
			for(Object o : root.getChildren()) {
				Element e = (Element)o;
				if(e.getName().equals("resource")) {
					name = e.getAttributeValue("name");
					value = extractFormattedText(e);
					resources.put(name, value);
				}
			}
			return root;
		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
    
	public WString getCommonValue(String form, String item) {
		return new WString(extractFormattedText(common.getChild(form).getChild(item)));
	}
	
	public WString getOrganismValue(String form, String item) {
		Element formE = organism.getChild(form);
		if (formE != null && formE.getChild(item) != null)
			return new WString(extractFormattedText(formE.getChild(item)));
		
		return null;
	}
	
	public WString getOrganismValue(String form, String item, List<String> args) {
		String value = extractFormattedText(organism.getChild(form).getChild(item));
		
		for(int i = 0; i<args.size(); i++) {
			value = value.replace("{"+(i+1)+"}", args.get(i));
		}
		
		return new WString(value);
	}
	
	public Element getOrganismElement(String form, String item) {
		return organism.getChild(form).getChild(item);
	}
	
	public String getOrganismElementAsString(String form, String item) {
		try {
			XMLOutputter outputter = new XMLOutputter();
		    StringWriter sw = new StringWriter();
		    
		    for (Object e : organism.getChild(form).getChild(item).getContent()) {
		    	if (e instanceof Text) {
		    		outputter.output((Text)e, sw);		
		    	} else if (e instanceof Element) {
		    		outputter.output((Element)e, sw);		
		    	} else {
		    		throw new RuntimeException("Element " + e.getClass().toString() + " not supported");
		    	}
		    }
		    
		    return sw.toString();
		} catch (Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	public String extractFormattedText(Element child) {
		StringBuilder textToReturn = new StringBuilder();
		extractFormattedText(textToReturn, child);
		String result = textToReturn.toString().trim();
		if (result.charAt(result.length()-1)==':')
			result += ' ';
		return result.replaceAll("\\$\\{app.url\\}", WApplication.getInstance().resolveRelativeUrl(WApplication.getInstance().getBookmarkUrl("/")));
	}
	
	private void extractFormattedText(StringBuilder textToReturn, Element child) {
		for(Object o : child.getContent()) {
			if(o instanceof Text) {
				textToReturn.append(WWebWidget.escapeText(((Text)o).getText(), false));
			} else {
				Element e = (Element)o;
				textToReturn.append("<"+e.getName());
				for(Object oa : e.getAttributes()) {
					Attribute a = (Attribute)oa;
					textToReturn.append(" "+ a.getName() + "=\"" + WWebWidget.escapeText(a.getValue(), false) + "\"");
				}
				textToReturn.append(">");
				extractFormattedText(textToReturn, e);
				textToReturn.append("</"+e.getName()+">");
			}
		}
	}

	public void refresh() {
		common = processDoc(commonXml);
		organism = processDoc(organismXml);
	}

	@Override
	public String resolveKey(String key) {
		return resources.get(key);
	}

	public boolean haveForm(String form) {
		return organism.getChild(form) != null;
	}
}
