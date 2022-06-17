package org.openmrs.module.formdataexport.extension.html;

import java.util.LinkedHashMap;
import java.util.Map;

import org.openmrs.module.Extension;
import org.openmrs.module.web.extension.AdministrationSectionExt;

public class FormDataExportAdminExt extends AdministrationSectionExt {

	public Extension.MEDIA_TYPE getMediaType() {
		return Extension.MEDIA_TYPE.html;
	}
	
	public String getTitle() {
		return "formdataexport.title";
	}
	
	public String getRequiredPrivilege() {
		return "Manage Form Exports, Manage Reports, Manage Forms";
	}
	
	public Map<String, String> getLinks() {
		
		Map<String, String> map = new LinkedHashMap<String, String>();
		map.put("module/formdataexport/formDataExport.list", "formdataexport.FormDataExport.title");
		map.put("module/formdataexport/countForms.list", "formdataexport.countForms.title");
		map.put("module/formdataexport/userDataExport.list", "formdataexport.UserDataExport.title");
		return map;
	}
	
}
