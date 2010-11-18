package org.openmrs.module.formdataexport.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.PatientIdentifierType;
import org.openmrs.api.context.Context;
import org.openmrs.module.htmlformentry.FormEntrySession;
import org.openmrs.module.htmlformentry.FormSubmissionController;
import org.openmrs.module.htmlformentry.HtmlForm;
import org.openmrs.module.htmlformentry.HtmlFormEntryUtil;
import org.openmrs.module.htmlformentry.FormEntryContext.Mode;
import org.openmrs.module.htmlformentry.action.FormSubmissionControllerAction;
import org.openmrs.module.htmlformentry.element.ObsSubmissionElement;
import org.openmrs.module.htmlformentry.schema.HtmlFormField;
import org.openmrs.module.htmlformentry.schema.HtmlFormSchema;
import org.openmrs.module.htmlformentry.schema.ObsField;
import org.openmrs.module.htmlformentry.schema.ObsGroup;

public class HtmlFormEntryExportUtil {

    protected final static Log log = LogFactory.getLog(HtmlFormEntryExportUtil.class);
    
    private static DateFormat DATE_FORMATTER = new SimpleDateFormat("dd-MMM-yyyy");
    
    private static final String DEFAULT_QUOTE = "\"";

    private static final String DEFAULT_COLUMN_SEPARATOR = "|";

    private static final String DEFAULT_LINE_SEPARATOR = "\n";
    
    private static final String EMPTY = "";
    
    /**
     * 
     * Generate the header row for the csv file.
     * 
     * @param form
     * @param extraCols
     * @return
     * @throws Exception
     */ 
    public static String generateColumnHeadersFromHtmlForm(HtmlForm form, List<String> extraCols, StringBuffer sb, PatientIdentifierType pit) throws Exception {
        FormEntrySession session = new FormEntrySession(HtmlFormEntryUtil.getFakePerson(), form);
        HtmlFormSchema hfs = session.getContext().getSchema();
        if (pit == null)
            throw new RuntimeException("Please provide a patient identifier type for this export.");
        
        sb.
        append(DEFAULT_QUOTE).append("ENCOUNTER_ID").append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR).
        append(DEFAULT_QUOTE).append("ENCOUNTER_DATE").append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR).
        append(DEFAULT_QUOTE).append("ENCOUNTER_LOCATION").append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR).
        append(DEFAULT_QUOTE).append("ENCOUNTER_PROVIDER").append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR).
        append(DEFAULT_QUOTE).append("INTERNAL_PATIENT_ID").append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR).
        append(DEFAULT_QUOTE).append(pit.getName()).append(DEFAULT_QUOTE);
        
        for (HtmlFormField hfsec : hfs.getAllFields())
                sb = generateColumnHeadersFromHtmlFormHelper(hfsec, extraCols, sb);

        session = null;
        sb.append(DEFAULT_LINE_SEPARATOR);
        return sb.toString();
    }
    
    private static StringBuffer generateColumnHeadersFromHtmlFormHelper(HtmlFormField hff, List<String> extraCols, StringBuffer sb){
        if (hff instanceof ObsField){
            ObsField of = (ObsField) hff;      
            sb = buildHeadersForObsField(of, extraCols, sb);
        } else if (hff instanceof ObsGroup){
                ObsGroup og = (ObsGroup) hff;
                for (HtmlFormField of : og.getChildren()){
                    sb = generateColumnHeadersFromHtmlFormHelper(of, extraCols, sb);
                }
        }
        return sb;
    }
    
    /**
     * 
     * Builds the root column name for the concept from the conceptID
     * 
     * @param of
     * @return
     */
    private static String buildColumnHeader(ObsField of){
        StringBuilder sb = new StringBuilder(EMPTY);
        Locale loc = Context.getLocale();
        if (of.getQuestion() != null){
            //TODO: add fieldId, fieldPart, Page???
            sb.append(of.getQuestion().getBestShortName(loc));
        } else if (of.getAnswers().size() == 1){
            sb.append(of.getAnswers().get(0).getConcept().getBestShortName(loc));
        } else {
            throw new RuntimeException("Obs Field has no conceptId, and multiple answers -- this isn't yet supported.");
        }
        return sb.toString().replaceAll("\\s", "_").replaceAll("-", "_").toUpperCase();
    }
    
    /**
     * 
     * Adds all of the columns for an Obs Field.
     * 
     * @param of
     * @param extraCols
     * @param sb
     * @return
     */
    private static StringBuffer buildHeadersForObsField(ObsField of, List<String> extraCols, StringBuffer sb){
        
        sb.append(DEFAULT_COLUMN_SEPARATOR);
        sb.append(DEFAULT_QUOTE);
        String columnHeader = buildColumnHeader(of);
        sb.append(columnHeader);
        sb.append(DEFAULT_QUOTE);
    
        sb.append(DEFAULT_COLUMN_SEPARATOR);
        sb.append(DEFAULT_QUOTE);     
        sb.append(columnHeader + "_DATE");
        sb.append(DEFAULT_QUOTE);
        
      //always export obsGroupId
        sb.append(DEFAULT_COLUMN_SEPARATOR);
        sb.append(DEFAULT_QUOTE);     
        sb.append(columnHeader + "_PARENT");
        sb.append(DEFAULT_QUOTE);
        
        if (extraCols != null){
            for (String st : extraCols){
                if (st.equals("valueModifier")){
                    sb.append(DEFAULT_COLUMN_SEPARATOR);
                    sb.append(DEFAULT_QUOTE);        
                    sb.append(columnHeader + "_VALUE_MOD");                                   
                    sb.append(DEFAULT_QUOTE);
                } else if (st.equals("accessionNumber")){
                    sb.append(DEFAULT_COLUMN_SEPARATOR);
                    sb.append(DEFAULT_QUOTE);        
                    sb.append(columnHeader + "_ACCESSION_NUM");                                   
                    sb.append(DEFAULT_QUOTE);
                } else if (st.equals("comment")){
                    sb.append(DEFAULT_COLUMN_SEPARATOR);
                    sb.append(DEFAULT_QUOTE);        
                    sb.append(columnHeader + "_COMMENT");                                   
                    sb.append(DEFAULT_QUOTE);
                }
            }
        }
        
        return sb;
    }
    
    /**
     * 
     * Generates all of the data rows
     * 
     * @param form
     * @param extraCols
     * @param sb
     * @return
     * @throws Exception
     */
    public static String generateColumnDataFromHtmlForm(List<Encounter> encounters, HtmlForm form, List<String> extraCols, StringBuffer sb, Locale locale, PatientIdentifierType pit) throws Exception {
        for (Encounter e: encounters){
            
            sb.append(DEFAULT_QUOTE).append(e.getEncounterId()).append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);         
            sb.append(DEFAULT_QUOTE).append(DATE_FORMATTER.format(e.getEncounterDatetime())).append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);
            sb.append(DEFAULT_QUOTE).append(e.getLocation().getName()).append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);
            sb.append(DEFAULT_QUOTE).append(e.getProvider().getGivenName()+ " " + e.getProvider().getFamilyName()).append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);
            sb.append(DEFAULT_QUOTE).append((e.getPatient() != null ? e.getPatient().getPatientId() : EMPTY)).append(DEFAULT_QUOTE).append(DEFAULT_COLUMN_SEPARATOR);          
            sb.append(DEFAULT_QUOTE).append(e.getPatient().getPatientIdentifier(pit)).append(DEFAULT_QUOTE);
            
            
            FormEntrySession session = new FormEntrySession(e.getPatient(), e, Mode.VIEW, form);
            FormSubmissionController  fsa = session.getSubmissionController();
            List<FormSubmissionControllerAction> actions = fsa.getActions();
            for (FormSubmissionControllerAction fsca : actions){
                if (fsca instanceof ObsSubmissionElement){
                    ObsSubmissionElement ose = (ObsSubmissionElement) fsca;
                    sb = appendObsToRow(ose, sb, extraCols, locale);   
                } else {
                    //TODO: add programs, orders, logic, etc...
                    // just make sure these are in the headers too...
                }
            }
            session = null;
            sb.append(DEFAULT_LINE_SEPARATOR);
        }
        return sb.toString();
    }
    
    /**
     * 
     * Writes the row entries for the Obs
     * 
     * @param o
     * @param sb
     * @param extraCols
     * @param rowStarted
     * @return
     */
    private static StringBuffer appendObsToRow(ObsSubmissionElement ose, StringBuffer sb, List<String> extraCols, Locale locale){
            Obs o = ose.getExistingObs();       

            sb.append(DEFAULT_COLUMN_SEPARATOR);
            sb.append(DEFAULT_QUOTE);
            if (ose.getConcept() != null)
                sb.append((o != null) ? o.getValueAsString(locale):EMPTY);
            else 
                sb.append((o != null) ? o.getConcept().getBestName(locale):EMPTY);
            sb.append(DEFAULT_QUOTE);
            
            sb.append(DEFAULT_COLUMN_SEPARATOR);
            sb.append(DEFAULT_QUOTE);     
            sb.append((o!=null)? Context.getDateFormat().format(o.getObsDatetime()):EMPTY);                                   
            sb.append(DEFAULT_QUOTE); 
            
            sb.append(DEFAULT_COLUMN_SEPARATOR);
            sb.append(DEFAULT_QUOTE);     
            sb.append(getObsGroupPath(o));                                   
            sb.append(DEFAULT_QUOTE);
            
            if (extraCols != null){
                for (String st : extraCols){
                    if (st.equals("valueModifier")){
                        sb.append(DEFAULT_COLUMN_SEPARATOR);
                        sb.append(DEFAULT_QUOTE);        
                        sb.append((o != null && o.getValueModifier() != null) ? o.getValueModifier():EMPTY);                                   
                        sb.append(DEFAULT_QUOTE);
                    } else if (st.equals("accessionNumber")){
                        sb.append(DEFAULT_COLUMN_SEPARATOR);
                        sb.append(DEFAULT_QUOTE);        
                        sb.append((o != null && o.getAccessionNumber() != null) ? o.getAccessionNumber():EMPTY);                                   
                        sb.append(DEFAULT_QUOTE);
                    } else if (st.equals("comment")){
                        sb.append(DEFAULT_COLUMN_SEPARATOR);
                        sb.append(DEFAULT_QUOTE);        
                        sb.append((o != null && o.getComment() != null) ? o.getComment():EMPTY);                                   
                        sb.append(DEFAULT_QUOTE);
                    }
                }
            }
        return sb;
    }
    
    private static String getObsGroupPath(Obs o){
        StringBuilder st = new StringBuilder(EMPTY);
        if (o != null)
            while (o.getObsGroup() != null){
                o = o.getObsGroup();
                st.insert(0, o.getObsId() + ":");
            }
        return st.toString();
    }

}
