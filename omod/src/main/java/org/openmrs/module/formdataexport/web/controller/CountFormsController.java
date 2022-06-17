package org.openmrs.module.formdataexport.web.controller;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openmrs.Form;
import org.openmrs.Program;
import org.openmrs.Cohort;
import org.openmrs.api.context.Context;
import org.openmrs.module.formdataexport.FormDataExportService;
import org.openmrs.propertyeditor.ProgramEditor;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

public class CountFormsController extends SimpleFormController {
	
	@Override
    protected void initBinder(HttpServletRequest request, ServletRequestDataBinder binder) throws Exception {
    	super.initBinder(request, binder);
    	binder.registerCustomEditor(Program.class, new ProgramEditor());
    }
	
	@Override
	protected Object formBackingObject(HttpServletRequest arg0) throws Exception {
		return new CountFormsCommand();
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object obj, BindException errors) throws Exception {
		CountFormsCommand command = (CountFormsCommand) obj;
		if (Context.isAuthenticated()) {
			FormDataExportService service = (FormDataExportService) Context.getService(FormDataExportService.class);
			Cohort cohort = null;
			if (command.getProgram() != null) {
				// devo fazer um metodo para trocar isto
				//cohort = Context.getPatientSetService().getPatientsByProgramAndState(command.getProgram(), null, null, null);
			}
			command.setResults(service.countForms(cohort));
		}
		return new ModelAndView(getFormView(), getCommandName(), command);
	}

	@Override
	protected Map referenceData(HttpServletRequest request) throws Exception {
		Map ret = new HashMap();
		return ret;
	}

	public class CountFormsCommand {

		private Program program;
		private Map<Form, Integer> results;

		public Program getProgram() {
			return program;
		}

		public void setProgram(Program program) {
			this.program = program;
		}

		public Map<Form, Integer> getResults() {
			return results;
		}

		public void setResults(Map<Form, Integer> results) {
			this.results = results;
		}
		
	}
	
}
