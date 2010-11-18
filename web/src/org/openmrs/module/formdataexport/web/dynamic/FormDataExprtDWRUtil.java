package org.openmrs.module.formdataexport.web.dynamic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Form;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.HtmlFormEntryService;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.export.HtmlFormEntryExportUtil;

public class FormDataExprtDWRUtil {

    protected final Log log = LogFactory.getLog(getClass());
    
    public List<HtmlFormSectionItem> getFormSections(Integer formId){
        List<HtmlFormSectionItem> ret = new ArrayList<HtmlFormSectionItem>();
        Form form = Context.getFormService().getForm(formId);
        if (Context.getService(HtmlFormEntryService.class).getHtmlFormByForm(form) != null){
            try {
                Map<Integer, String>  map = HtmlFormEntryExportUtil.getSectionIndex(HtmlFormEntryUtil.getService().getHtmlFormByForm(form));
                for (Map.Entry<Integer, String> e : map.entrySet())
                    ret.add(new HtmlFormSectionItem(e.getKey(), e.getValue()));
            } catch (Exception ex){
                log.error("Unable to load htmlform to get sections.", ex);
                return ret;
            }
        } else {
            return ret;
        }
        return ret;
    }
    
}
