package org.openmrs.module.formdataexport.web.controller;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Form;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.springframework.web.servlet.mvc.SimpleFormController;

public class FormDataExportFormController extends SimpleFormController {
	
    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());

	/**
	 * This is called prior to displaying a form for the first time.  It tells Spring
	 *   the form/command object to load into the request
	 * 
	 * @see org.springframework.web.servlet.mvc.AbstractFormController#formBackingObject(javax.servlet.http.HttpServletRequest)
	 */
    protected Object formBackingObject(HttpServletRequest request) throws ServletException {
    	Form form = null;
		
		if (Context.isAuthenticated()) {
			FormService fs = (FormService)Context.getService(FormService.class);
			String id = request.getParameter("formId");
	    	if (id != null)
	    		form = fs.getForm(new Integer(id));	
		}
		
		if (form == null)
			form = new Form();
    	
        return form;
    }
    
}