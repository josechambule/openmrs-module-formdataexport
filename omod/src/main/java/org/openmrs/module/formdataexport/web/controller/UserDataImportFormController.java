/**
 * 
 */
package org.openmrs.module.formdataexport.web.controller;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.Privilege;
import org.openmrs.Role;
import org.openmrs.User;
import org.openmrs.api.UserService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.LoginCredential;
import org.openmrs.module.formdataexport.UserDataExportService;
import org.openmrs.module.formdataexport.model.FileUpload;

/**
 * @author jose.chambule
 *
 */
@SuppressWarnings("deprecation")
@Controller
public class UserDataImportFormController extends SimpleFormController {

	protected Log log;
	private String txt = "";
	
	private UserDataExportService userDataExportService;

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
		request.setAttribute("txtefile", txt);
		return new FileUpload();
	}
	
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command,
			BindException errors) throws Exception {
		try {
			txt = "teste2";
            FileUpload bean = (FileUpload) command;
            MultipartFile file = bean.getFile();
			if (file == null) {
				txt = "teste3";
			}else {
				txt = file.getOriginalFilename()+" size 2";
				HSSFWorkbook workbook = new HSSFWorkbook(file.getInputStream());
				HSSFSheet sheet = (HSSFSheet) workbook.getSheetAt(0);
				readExcelFile(sheet);
				Row row;
				for (int i = 1; i <= sheet.getLastRowNum(); i++) { 
					row = (Row) sheet.getRow(i); // sheet number
					if (row.getCell(0) == null) {
						txt = file.getSize() + " size 3";
					}
					else {
						txt = row.getCell(0).toString();
						request.setAttribute("txtefile", file.getOriginalFilename() +" "+ row.getCell(0).toString());
					} // else copies cell data to name variable
				}
			}
		} catch (Exception e) {
			log.error("An error occured while import data from ", e);
			request.getSession().setAttribute("openmrs_error",
					("formdataexport.FormDataExport.GeneralError: " + e.getMessage()));
		}
		ModelMap map = new ModelMap();
		request.setAttribute("txtfile", txt);
		map.put("txtefile", txt);
		return new ModelAndView(this.getSuccessView());
	}
	
	public void readExcelFile(HSSFSheet sheet) {
		Row row;
		for (int i = 1; i <= sheet.getLastRowNum(); i++) { 
			row = (Row) sheet.getRow(i); // sheet number
			User user = new User();
			LoginCredential loginCredential = new LoginCredential();
			Person person = new Person();
			PersonName personName = new PersonName();
			Role role = new Role();
			Privilege privilege = new Privilege();
			if (row.getCell(0) != null) {
				user.setUserId((int) row.getCell(0).getNumericCellValue());
			}
			if (row.getCell(1) != null) {
				user.setSystemId(row.getCell(1).toString());
			}
			if (row.getCell(2) != null) {
				loginCredential.setHashedPassword(row.getCell(2).toString());
				loginCredential.setUserId((int) row.getCell(0).getNumericCellValue());
			}
			if (row.getCell(3) != null) {
				loginCredential.setSalt(row.getCell(3).toString());
			}
			if (row.getCell(4) != null) {
				loginCredential.setSecretQuestion(row.getCell(4).toString());
			}
			if (row.getCell(5) != null) {
				loginCredential.setSecretAnswer(row.getCell(5).toString());
			}
			if (row.getCell(6) != null) {
				user.setUsername(row.getCell(6).toString());
			}
			if (row.getCell(7) != null) {
				user.setRetired(Boolean.valueOf(row.getCell(7).getBooleanCellValue()));
			}
			if (row.getCell(8) != null) {
				user.setEmail(row.getCell(8).toString());
			}
			if (row.getCell(9) != null) {
				user.setUuid(row.getCell(9).toString());
			}
			if (row.getCell(10) != null) {
				//person.setPersonId(Integer.valueOf((int) row.getCell(10).getNumericCellValue()));
			}
			if (row.getCell(11) != null) {
				personName.setGivenName(row.getCell(11).toString());
			}
			if (row.getCell(12) != null) {
				personName.setFamilyName(row.getCell(12).toString());
			}
			if (row.getCell(13) != null) {
				personName.setMiddleName(row.getCell(13).toString());
			}
			if (row.getCell(14) != null) {
				person.setGender(row.getCell(14).toString());
			}
			if (row.getCell(15) != null) {
				person.setBirthdate(row.getCell(15).getDateCellValue());
			}
			if (row.getCell(16) != null) {
				person.setUuid(row.getCell(16).toString());
			}
			if (row.getCell(17) != null) {
				String roles = row.getCell(17).toString();
				String[] result = roles.split(",");
				
				Set<Role> rol = new HashSet<Role>();
				
				for(String rl : result) {
					role.setRole(rl);
					rol.add(role);					
				}	
				
				String privileges = row.getCell(16).toString();
				String[] resultPrivileges = privileges.split(",");
				
				Set<Privilege> priv = new HashSet<Privilege>();
				
				for(String pr : resultPrivileges) {
					privilege.setPrivilege(pr);
					priv.add(privilege);
				}
				role.setPrivileges(priv);
				
				user.setRoles(rol);
			}
			if (row.getCell(18) != null) {
				
			}
			person.addName(personName);
			user.setPerson(person);
			saveImportedData(user, loginCredential);
		}
	}
	
	public void saveImportedData(User user, LoginCredential credentials) {
		UserService us = Context.getUserService();
		if(us.getUserByUuid(user.getUuid()) == null) {
			userDataExportService.importUser(user, credentials);
		}
	}
}
