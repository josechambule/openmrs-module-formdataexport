/**
 * 
 */
package org.openmrs.module.formdataexport.web.controller;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.Provider;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.LoginCredential;
import org.openmrs.module.formdataexport.UserDataExportService;
import org.openmrs.module.formdataexport.model.FileUpload;
import org.openmrs.util.OpenmrsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

/**
 * @author jose.chambule
 *
 */
@SuppressWarnings("deprecation")
@Controller
public class UserDataImportFormController extends SimpleFormController {

	protected Log log;
	private int count, contador = 0;
	
	private UserDataExportService userDataExportService;
	private File importLogFile;
	private BufferedWriter imporLogBuffer;
	private FileWriter importLogWriter;
	@Autowired
	public void setUserDataExportService(UserDataExportService userDataExportService) {
		this.userDataExportService = userDataExportService;
	}

	public UserDataImportFormController() {
		// TODO Auto-generated constructor stub
		log = LogFactory.getLog(this.getClass());
	}

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws ServletException {
		request.setAttribute("txtfile", "");
		return new FileUpload();
	}
	
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command,
			BindException errors) throws Exception {
		
		try {
			this.importLogFile = new File(OpenmrsUtil.getApplicationDataDirectory() + File.separator + "user_import_error.log"); 
			this.importLogWriter = new FileWriter(this.importLogFile, true);
			this.imporLogBuffer = new BufferedWriter(importLogWriter);
			
            FileUpload bean = (FileUpload) command;
            MultipartFile file = bean.getFile();
			if (file == null) {
			}else {
				HSSFWorkbook workbook = new HSSFWorkbook(file.getInputStream());
				HSSFSheet sheet = (HSSFSheet) workbook.getSheetAt(0);
				readExcelFile(sheet);
				request.setAttribute("txtfile", (count-contador) + " Utilizadores importados!");
			}
		} catch (Exception e) {
			log.error("An error occured while import data from ", e);
			request.getSession().setAttribute("openmrs_error",
					("formdataexport.FormDataExport.GeneralError: " + e.getMessage()));
		}
		finally {
			this.imporLogBuffer.close();
			this.importLogWriter.close();
		}
		
		count=0;
		contador=0;
		return new ModelAndView(this.getSuccessView());
	}
	
	public void readExcelFile(HSSFSheet sheet) {
		
		Row row = null;
		String logText = null;
		
		for (int i = 1; i <= sheet.getLastRowNum(); i++) { 
			try {
				log("Starting extraction of user list line [" + i + "]"); 
				
				FileUtils.writeStringToFile(this.importLogFile, logText);
				
				row = (Row) sheet.getRow(i);
				User user = new User();
				LoginCredential loginCredential = new LoginCredential();
				Person person = new Person();
				PersonName personName = new PersonName();
				Provider provider = new Provider();
				
				if (row.getCell(0) != null) {
					user.setUserId((int) row.getCell(0).getNumericCellValue());
				}
				if (row.getCell(1) != null) {
					user.setSystemId(trimToNull(row.getCell(1).toString()));
				}
				if (row.getCell(2) != null) {
					loginCredential.setHashedPassword(trimToNull(row.getCell(2).toString()));
					loginCredential.setUserId((int) row.getCell(0).getNumericCellValue());
				}
				if (row.getCell(3) != null) {
					loginCredential.setSalt(trimToNull(row.getCell(3).toString()));
				}
				if (row.getCell(4) != null) {
					loginCredential.setSecretQuestion(trimToNull(row.getCell(4).toString()));
				}
				if (row.getCell(5) != null) {
					loginCredential.setSecretAnswer(trimToNull(row.getCell(5).toString()));
				}
				if (row.getCell(6) != null) {
					user.setUsername(trimToNull(row.getCell(6).toString()));
				}
				if (row.getCell(7) != null) {
					user.setRetired(Boolean.valueOf(row.getCell(7).getBooleanCellValue()));
				}
				if (row.getCell(8) != null) {
					user.setEmail(trimToNull(row.getCell(8).toString()));			
				}
				if (row.getCell(9) != null) {
					user.setUuid(trimToNull(row.getCell(9).toString()));
				}
				if (row.getCell(10) != null) {
					//person.setPersonId(Integer.valueOf((int) row.getCell(10).getNumericCellValue()));
				}
				if (row.getCell(11) != null) {
					personName.setGivenName(trimToNull(row.getCell(11).toString()));
				}
				if (row.getCell(12) != null) {
					personName.setFamilyName(trimToNull(row.getCell(12).toString()));
				}
				if (row.getCell(13) != null) {
					personName.setMiddleName(trimToNull(row.getCell(13).toString()));
				}
				if (row.getCell(14) != null) {
					person.setGender(trimToNull(row.getCell(14).toString()));
				}
				if (row.getCell(15) != null) {
					//person.setBirthdate(row.getCell(15).getDateCellValue());
				}
				if (row.getCell(16) != null) {
					person.setUuid(trimToNull(row.getCell(16).toString()));
				}
				if (row.getCell(17) != null) {
					String roles = removeFirstandLast(row.getCell(17).toString());
					String[] result = roles.split(", ");
					
					Set<Role> rol = new HashSet<Role>();
					
					for(String rl : result) {
						if(Context.getUserService().getRole(rl)!=null) {
							rol.add(Context.getUserService().getRole(rl));
						}										
					}	
										
					user.setRoles(rol);
				}
				if (row.getCell(18) != null) {
					
				}
				if (row.getCell(19) != null) {
					user.setRetireReason(trimToNull(row.getCell(19).toString()));
				}
				if (row.getCell(20) != null) {
					personName.setUuid(trimToNull(row.getCell(20).toString()));
				}
				if (row.getCell(21) != null) {
					provider.setIdentifier(trimToNull(row.getCell(21).toString()));
					provider.setName(personName.getFullName());
					provider.setCreator(Context.getAuthenticatedUser());
				}
				if (row.getCell(22) != null) {
					provider.setUuid(trimToNull(row.getCell(22).toString()));
				}
				if (row.getCell(23) != null) {
					provider.setRetired(row.getCell(23).getBooleanCellValue());
				}
				if (row.getCell(24) != null) {
					provider.setRetireReason(trimToNull(row.getCell(24).toString()));
				}
				
				if(Context.getPersonService().getPersonByUuid(person.getUuid())!=null) {
					user.setPerson(Context.getPersonService().getPersonByUuid(person.getUuid()));
				}else {
					person.addName(personName);
					user.setPerson(person);
				}
				
				saveImportedData(user, loginCredential, personName);
				
				if (provider.getUuid() != null) {
					if(Context.getProviderService().getProviderByUuid(provider.getUuid())==null) {
						provider.setPerson(user.getPerson());
						Context.getProviderService().saveProvider(provider);
					}
				}
				count++;
			}
			catch (Exception e) {
				log("An error ocurred Processing row :  " + i + " Error: " + e.getLocalizedMessage());	
			}
		}
	}
	
	public String removeFirstandLast(String str)
    {
        str = str.substring(1, str.length() - 1);
        return str;
    }
	
	public String trimToNull(String str) {
		return StringUtils.trimToNull(str);
	}
	
	public void saveImportedData(User user, LoginCredential credentials, PersonName personName) {
		try {
			log("Processing Usert:  " + user.getUsername() + " UUID: " + user.getUuid());
			
			UserService us = Context.getUserService();
			User usr = us.getUserByUuid(user.getUuid());
			if(usr == null) {
				user.setUserId(null);
				user.setUserProperty("forcePassword", "false");
				if(user.getSystemId() == null) {
					user.setSystemId("UNDEFINED_SystemId_" + us.generateSystemId());
				}
				userDataExportService.importUser(user, credentials);
			}else {	
				usr.setRetired(user.getRetired());
				usr.setUsername(user.getUsername());
				usr.setSystemId(user.getSystemId());
				usr.setEmail(user.getEmail());
				usr.setRetireReason(user.getRetireReason());
				usr.setRoles(user.getRoles());
				usr.setUserProperty("forcePassword", "false");
				if(usr.getSystemId() == null) {
					usr.setSystemId("UNDEFINED_SystemId_" + us.generateSystemId());
				}
				userDataExportService.importUser(usr, credentials);
				
				if(personName.getUuid() != null) {
					PersonName pn = Context.getPersonService().getPersonNameByUuid(personName.getUuid());
					pn.setFamilyName(personName.getFamilyName());
					pn.setGivenName(personName.getGivenName());
					pn.setMiddleName(personName.getMiddleName());
					
					Context.getPersonService().savePersonName(pn);
				}
			}
		}
		catch (Exception e) {
			String logText = "An error ocurred Processing User:  " + user.getUsername() + " UUID: " + user.getUuid() + " Error: " + e.getLocalizedMessage();
			
			log(logText);
		}
	}
	
	private void log(String logText) {
		System.out.println(logText);	
		
		//log.info(logText);	
		
		//try {
			//imporLogBuffer.write(logText);
			//imporLogBuffer.flush();
		//}
		//catch (IOException e1) {
			//e1.printStackTrace();
		//}
	}
}
