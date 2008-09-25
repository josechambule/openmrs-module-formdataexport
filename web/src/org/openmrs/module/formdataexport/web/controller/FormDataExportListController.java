package org.openmrs.module.formdataexport.web.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.EncounterType;
import org.openmrs.Form;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.cohort.CohortDefinition;
import org.openmrs.cohort.CohortDefinitionItemHolder;
import org.openmrs.module.formdataexport.FormDataExportService;
import org.openmrs.web.WebConstants;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

public class FormDataExportListController extends SimpleFormController {
	
    /** Logger for this class and subclasses */
    protected final Log log = LogFactory.getLog(getClass());

    /**
     * Returns the model object for the form view.
     */
    protected Object formBackingObject(HttpServletRequest request) throws ServletException {
		
		//default empty Object
		List<Form> formList = new Vector<Form>();
		
		//only fill the Object is the user has authenticated properly
		if (Context.isAuthenticated()) {
			FormService fs = (FormService) Context.getFormService();
	    	return fs.getAllForms();
		}
    	
        return formList;
    }
    
    /**
     * Processes the form submission.
     */
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException errors) { 
    	log.info("Processing form data export submission");
    	Form form = null;
    	
    	try { 
    		int formId = ServletRequestUtils.getRequiredIntParameter(request, "formId");
    		form = Context.getFormService().getForm(formId);
    	} 
    	catch (ServletRequestBindingException e){ 
    		log.error("Could not parse form id", e);
    	}

		// Get other request parameters
		String cohortKey = ServletRequestUtils.getStringParameter(request, "cohortKey", "");
		String [] extraColumns = ServletRequestUtils.getStringParameters(request, "extraColumn");
    	    	
		try { 
			
			
			
			if ( form != null ) { 
				// Export data
				File exportFile = 
					getFormDataExportService().exportEncounterData(form, cohortKey, extraColumns);
				
				// Check if the export file exists
				if (exportFile == null || !exportFile.exists()) { 
					throw new ServletException("The data export has not been generated");
				}
				// If it does, then we write it to the responses
				else { 				
					String timestamp = new SimpleDateFormat("yyyyMMdd_Hm").format(new Date());
					//String filename = exportFile.getName().replace(" ", "_") + "-" + timestamp + ".xls";
					String filename = exportFile.getName().replace(" ", "_") + "-" + timestamp + ".csv";
					response.setHeader("Content-Disposition", "attachment; filename=" + filename);
					//application/vnd.ms-excel
					response.setContentType("text/csv");
					response.setHeader("Pragma", "no-cache");
	
					InputStream inputStream = null;
					try { 
						
						inputStream = new FileInputStream(exportFile);
						FileCopyUtils.copy(inputStream, response.getOutputStream());
						
					} 
					finally { 
						if (inputStream != null) { 
							try { 
								inputStream.close(); 
							} 
							catch (Exception e) { log.error("Error closing export file ", e);}
						}
					}
					return null;				
				} 			
			} else { 
				
				request.getSession().setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "formdataexport.FormDataExport.FormNotFoundError");
			}
		} 
		catch (Exception e) { 
			log.error("An error occured while export form data ", e);
			request.getSession().setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "formdataexport.FormDataExport.GeneralError");
		}    	
    	return new ModelAndView(getSuccessView());
    }


	/**
	 * Gets the reference data required for the reporting use cases implemented within this controller.
	 * 
	 * @param	request
	 * @param	command
	 * @param	errors
	 * @return	a map containing data used in the presentation layer
	 */
    protected Map referenceData(HttpServletRequest request, Object command, Errors errors) {
		Map<Object, Object> data = new HashMap<Object, Object>();
		List<CohortDefinitionItemHolder> cohortList = Context.getCohortService().getAllCohortDefinitions();
    	data.put("cohortList", cohortList);
    	return data;
    }
    

    
    /**
     * 
     * @return
     */
    public FormDataExportService getFormDataExportService() {     
		return (FormDataExportService) Context.getService(FormDataExportService.class);
    }
	
	
}