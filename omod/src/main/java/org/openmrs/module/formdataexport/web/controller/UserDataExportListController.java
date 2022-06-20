package org.openmrs.module.formdataexport.web.controller;

import org.openmrs.module.formdataexport.FormDataExportService;
import org.openmrs.module.formdataexport.UserDataExportService;

import javax.servlet.ServletRequest;
import org.springframework.web.bind.ServletRequestUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import java.io.OutputStream;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import java.io.ByteArrayOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import org.openmrs.api.UserService;
import java.util.List;
import java.util.ArrayList;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.LoginCredential;
import org.openmrs.User;
import java.util.Vector;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.springframework.web.servlet.mvc.SimpleFormController;

@SuppressWarnings("deprecation")
@Controller
public class UserDataExportListController extends SimpleFormController {
	protected Log log;
	
	private UserDataExportService userDataExportService;

	@Autowired
	public void setUserDataExportService(UserDataExportService userDataExportService) {
		this.userDataExportService = userDataExportService;
	}

	public UserDataExportListController() {
		log = LogFactory.getLog(this.getClass());
	}

	@Override
	protected Object formBackingObject(final HttpServletRequest request) throws ServletException {
		List<User> userList = new Vector<User>();
		if (Context.isAuthenticated()) {
			UserService us = Context.getUserService();
			List<User> listUser = new ArrayList<User>();
			List<User> list = new ArrayList<User>();
			String searchId = ServletRequestUtils.getStringParameter((ServletRequest) request, "searchId", "");

			if (searchId.equalsIgnoreCase("")) {
				listUser = us.getAllUsers();
			} else {
				listUser = userDataExportService.getUserByUsername(searchId);
			}

			int page = 1;
			int pointPage = 1;
			int recordsPerPage = 15;
			int noOfRecords = listUser.size();
			if (noOfRecords > 500) {
				if (noOfRecords < 1000) {
					recordsPerPage = 50;
				} else {
					recordsPerPage = 15;
				}
			}
			
			int noOfPages = (int) Math.ceil(noOfRecords * 1.0 / recordsPerPage);
			if (request.getParameter("recordsPerPage") != null) {
				recordsPerPage = Integer.parseInt(request.getParameter("recordsPerPage"));
			}
			if (request.getParameter("page") != null) {
				page = Integer.parseInt(request.getParameter("page"));
			}

			if (page == noOfPages) {
				for (int i = (page - 1) * recordsPerPage; i < noOfRecords; ++i) {
					list.add(listUser.get(i));
				}
			} else {
				for (int i = (page - 1) * recordsPerPage; i < page * recordsPerPage; ++i) {
					list.add(listUser.get(i));
				}
			}
			if (noOfPages > 30) {
				pointPage = 29;
			}

			request.setAttribute("searchId", (Object) searchId);
			request.setAttribute("userList", (Object) list);
			request.setAttribute("noOfPages", (Object) noOfPages);
			request.setAttribute("currentPage", (Object) page);
			request.setAttribute("pointPage", (Object) pointPage);
			request.setAttribute("recordsPerPageList", (Object) this.getListRecordsPerPage());
			request.setAttribute("recordsPerPage", (Object) recordsPerPage);
			return list;
		}
		return userList;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command,
			BindException errors) {

		try {
			String allUser = ServletRequestUtils.getStringParameter((ServletRequest) request, "searchId", "");
			List<User> userList = new ArrayList<User>();
			if (allUser.equalsIgnoreCase("")) {
				UserService us = Context.getUserService();
				userList = us.getAllUsers();
				createExcelFile(userList, response);
			} else {
				userList = (List<User>) request.getAttribute("userList");
				createExcelFile(userList, response);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			this.log.error("An error occured while export form data ", e);
			request.getSession().setAttribute("openmrs_error",
					("formdataexport.FormDataExport.GeneralError: " + e.getMessage()));
		}

		return new ModelAndView(this.getSuccessView());
	}

	public FormDataExportService getFormDataExportService() {
		return (FormDataExportService) Context.getService(FormDataExportService.class);
	}

	public class RecordsPerPage {
		private int value;

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}

		public RecordsPerPage(int value) {
			this.value = value;
		}
	}

	public List<RecordsPerPage> getListRecordsPerPage() {
		List<RecordsPerPage> recordsPerPageList = new ArrayList<RecordsPerPage>();
		recordsPerPageList.add(new RecordsPerPage(5));
		recordsPerPageList.add(new RecordsPerPage(10));
		recordsPerPageList.add(new RecordsPerPage(25));
		recordsPerPageList.add(new RecordsPerPage(50));
		recordsPerPageList.add(new RecordsPerPage(100));
		recordsPerPageList.add(new RecordsPerPage(150));
		return recordsPerPageList;
	}

	private void writeUserList(User user, Row row) {
		LoginCredential loginCredential = new LoginCredential();
		loginCredential = userDataExportService.getUserLoginCredential(user);
		Cell cell = row.createCell(0);
		cell.setCellValue((double) user.getUserId());
		cell = row.createCell(1);
		cell.setCellValue(user.getSystemId());
		cell = row.createCell(2);
		cell.setCellValue(loginCredential.getHashedPassword());
		cell = row.createCell(3);
		cell.setCellValue(loginCredential.getSalt());
		cell = row.createCell(4);
		cell.setCellValue(loginCredential.getSecretQuestion());
		cell = row.createCell(5);
		cell.setCellValue(loginCredential.getSecretAnswer());
		cell = row.createCell(6);
		cell.setCellValue(user.getUsername());
		cell = row.createCell(7);
		cell.setCellValue(user.getRetired());
		cell = row.createCell(8);
		cell.setCellValue(user.getEmail());
		cell = row.createCell(9);
		cell.setCellValue(user.getUuid());
		cell = row.createCell(10);
		cell.setCellValue((double) user.getPerson().getPersonId());
		cell = row.createCell(11);
		cell.setCellValue(user.getPerson().getGivenName());
		cell = row.createCell(12);
		cell.setCellValue(user.getPerson().getFamilyName());
		cell = row.createCell(13);
		cell.setCellValue(user.getPerson().getMiddleName());
		cell = row.createCell(14);
		cell.setCellValue(user.getPerson().getUuid());
		cell = row.createCell(15);
		cell.setCellValue(user.getAllRoles().toString());
		cell = row.createCell(16);
		cell.setCellValue(user.getPrivileges().toString());
	}

	public void createHeaderRow(Sheet sheet) {
		CellStyle cellStyle = sheet.getWorkbook().createCellStyle();
		cellStyle.setAlignment((short) 1);
		Font font = sheet.getWorkbook().createFont();
		font.setBoldweight((short) 2);
		font.setFontHeightInPoints((short) 12);
		cellStyle.setFont(font);
		cellStyle.setWrapText(true);
		Row row = sheet.createRow(0);
		Cell cellUserId = row.createCell(0);
		cellUserId.setCellStyle(cellStyle);
		cellUserId.setCellValue("UserId");
		Cell cellSystemId = row.createCell(1);
		cellSystemId.setCellStyle(cellStyle);
		cellSystemId.setCellValue("SystemId");
		Cell cellUserhashedPassword = row.createCell(2);
		cellUserhashedPassword.setCellStyle(cellStyle);
		cellUserhashedPassword.setCellValue("Password");
		Cell cellUsersalt = row.createCell(3);
		cellUsersalt.setCellStyle(cellStyle);
		cellUsersalt.setCellValue("Salt");
		Cell cellUsersecretQuestion = row.createCell(4);
		cellUsersecretQuestion.setCellStyle(cellStyle);
		cellUsersecretQuestion.setCellValue("Secret Question");
		Cell cellUsersecretAnswer = row.createCell(5);
		cellUsersecretAnswer.setCellStyle(cellStyle);
		cellUsersecretAnswer.setCellValue("Secret Answer");		
		Cell cellUserName = row.createCell(6);
		cellUserName.setCellStyle(cellStyle);
		cellUserName.setCellValue("UserName");
		Cell cellRetired = row.createCell(7);
		cellRetired.setCellStyle(cellStyle);
		cellRetired.setCellValue("Retired");
		Cell cellEmail = row.createCell(8);
		cellEmail.setCellStyle(cellStyle);
		cellEmail.setCellValue("Email");
		Cell cellUUID = row.createCell(9);
		cellUUID.setCellStyle(cellStyle);
		cellUUID.setCellValue("User UUID");
		Cell cellPersonId = row.createCell(10);
		cellPersonId.setCellStyle(cellStyle);
		cellPersonId.setCellValue("PersonId");
		Cell cellPersonName = row.createCell(11);
		cellPersonName.setCellStyle(cellStyle);
		cellPersonName.setCellValue("PersonName");
		Cell cellFamilyName = row.createCell(12);
		cellFamilyName.setCellStyle(cellStyle);
		cellFamilyName.setCellValue("FamilyName");
		Cell cellMiddleName = row.createCell(13);
		cellMiddleName.setCellStyle(cellStyle);
		cellMiddleName.setCellValue("MiddleName");
		Cell cellPersonUUID = row.createCell(14);
		cellPersonUUID.setCellStyle(cellStyle);
		cellPersonUUID.setCellValue("PersonUUID");
		Cell cellRoles = row.createCell(15);
		cellRoles.setCellStyle(cellStyle);
		cellRoles.setCellValue("Roles");
		Cell cellPrivileges = row.createCell(16);
		cellPrivileges.setCellStyle(cellStyle);
		cellPrivileges.setCellValue("Privileges");
	}

	public void createExcelFile(List<User> listUser, HttpServletResponse response) throws Exception {
		try (ByteArrayOutputStream outByteStream = new ByteArrayOutputStream()) {
			HSSFWorkbook workbook = new HSSFWorkbook();
			HSSFSheet sheet = workbook.createSheet("UserDataExport");
			sheet.setColumnWidth(0, 8000);
			sheet.setColumnWidth(1, 8000);
			sheet.setColumnWidth(2, 8000);
			sheet.setColumnWidth(3, 8000);
			sheet.setColumnWidth(4, 8000);
			sheet.setColumnWidth(5, 8000);
			sheet.setColumnWidth(6, 8000);
			sheet.setColumnWidth(7, 8000);
			sheet.setColumnWidth(8, 8000);
			createHeaderRow((Sheet) sheet);
			int rowCount = 0;
			for (User user : listUser) {
				Row row = (Row) sheet.createRow(++rowCount);
				this.writeUserList(user, row);
			}
			workbook.write((OutputStream) outByteStream);
			byte[] outArray = outByteStream.toByteArray();
			response.setContentType("application/ms-excel");
			response.setContentLength(outArray.length);
			response.setHeader("Expires:", "0");
			response.setHeader("Content-Disposition", "attachment; filename=User_Export_Data.xls");
			OutputStream outStream = (OutputStream) response.getOutputStream();
			outStream.write(outArray);
			outStream.flush();
			outStream.close();
		}
	}
}
