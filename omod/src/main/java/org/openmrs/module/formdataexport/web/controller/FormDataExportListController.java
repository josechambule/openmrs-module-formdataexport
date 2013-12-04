package org.openmrs.module.formdataexport.web.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.openmrs.Form;
import org.openmrs.api.FormService;
import org.openmrs.api.context.Context;
import org.openmrs.module.formdataexport.FormDataExportService;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.definition.DefinitionContext;
import org.openmrs.web.WebConstants;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

public class FormDataExportListController extends SimpleFormController {
	
    protected final Log log = LogFactory.getLog(getClass());


    @Override
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
    

    @Override
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
    	Integer sectionIndex = null;
        try {
            sectionIndex = ServletRequestUtils.getIntParameter(request, "section");
        } catch (Exception ex){
            //pass
        }
		String startDateStr = ServletRequestUtils.getStringParameter(request, "startDate", "");
		String endDateStr = ServletRequestUtils.getStringParameter(request, "endDate", "");
		String cohortQueryUUID = ServletRequestUtils.getStringParameter(request, "cohortUuid", "");
		String[] extraColumns = ServletRequestUtils.getStringParameters(request, "extraColumn");
		String firstLast = ServletRequestUtils.getStringParameter(request, "firstLast", "");
		Integer quantity = null;
		try {
		    quantity = ServletRequestUtils.getIntParameter(request, "quantity");
		} catch (Exception ex){
		    //pass
		}
		
		List<String> extraCols = new ArrayList<String>();
		for (int i = 0 ; i < extraColumns.length; i++){
		    extraCols.add(extraColumns[i]);
		}
		Date startDate = null;
		Date endDate = null;
		if (startDateStr != null && !startDateStr.equals("")){
    		try {
    		    startDate = Context.getDateFormat().parse(startDateStr);
    		} catch (Exception ex){
    		    throw new RuntimeException("The system couldn't parse the date you passed in for start date.");
    		}
		}
		if (endDateStr != null && !endDateStr.equals("")){
    		try {
    		    endDate = Context.getDateFormat().parse(endDateStr);
    		} catch (Exception ex){
    		    throw new RuntimeException("The system couldn't parse the date you passed in for end date.");
    		}
		}
		try { 
			
//			System.out.println("form " + form.getName());
//			System.out.println("sectionIndex " + sectionIndex);
//			System.out.println("startDateStr " + startDateStr);
//			System.out.println("endDateStr " + endDateStr);
//			System.out.println("cohortQueryUUID " + cohortQueryUUID);
//			System.out.println("extraCols " + extraCols);
//			System.out.println("firstLast " + firstLast);
//			System.out.println("quantity " + quantity);
			
			if ( form != null ) { 
				// Export data
				File exportFile = getFormDataExportService().exportEncounterData(form, sectionIndex, startDate, endDate, cohortQueryUUID, extraCols, firstLast,quantity);
				
				// Check if the export file exists
				if (exportFile == null || !exportFile.exists()) { 
					throw new ServletException("The data export has not been generated");
				}
				// If it does, then we write it to the responses
				else { 				
					String timestamp = new SimpleDateFormat("yyyyMMdd_Hm").format(new Date());
					//String filename = exportFile.getName().replace(" ", "_") + "-" + timestamp + ".xlsx";
					String filename = exportFile.getName().replace(" ", "_");
					//String filename = exportFile.getName().replace(" ", "_") + "-" + timestamp + ".csv";
					response.setHeader("Content-Disposition", "attachment; filename=" + filename);
					//application/vnd.ms-excel
					//response.setContentType("text/csv");
					response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
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
			request.getSession().setAttribute(WebConstants.OPENMRS_ERROR_ATTR, "formdataexport.FormDataExport.GeneralError: " + e.getMessage());
		}    	
    	return new ModelAndView(getSuccessView());
    }



    @Override
    protected Map referenceData(HttpServletRequest request, Object command, Errors errors) {
		Map<Object, Object> data = new HashMap<Object, Object>();
		//add list of cohort queries from new reporting framework:
		List<CohortDefinition> list = DefinitionContext.getAllDefinitions(CohortDefinition.class, false);
		for (CohortDefinition cd : list){
		    cd.getUuid();
		}
		data.put("cohorts", list);
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