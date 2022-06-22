/**
 * 
 */
package org.openmrs.module.formdataexport.web.controller;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;

/**
 * @author jose.chambule
 *
 */
@SuppressWarnings("deprecation")
@Controller
public class UserDataImportFormController extends SimpleFormController {

	protected Log log;
	private String txt = "";

	public UserDataImportFormController() {
		// TODO Auto-generated constructor stub
		log = LogFactory.getLog(this.getClass());
	}

	@Override
	protected Object formBackingObject(HttpServletRequest request) throws ServletException {
		List<FileItem> txtfile = new ArrayList<FileItem>();
		request.setAttribute("txtefile", txt);
		request.setAttribute("txtfile", txtfile);
		return txtfile;
	}

	@Override
	protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command,
			BindException errors) {
		try {
			txt = "teste2";
			boolean isMultipart = ServletFileUpload.isMultipartContent(request);
			if (!isMultipart) {
			} else {
				txt = "teste3";
				FileItemFactory factory = new DiskFileItemFactory();
				ServletFileUpload upload = new ServletFileUpload(factory);
				List<FileItem> items = null;
				try {
					items = upload.parseRequest(request);
					txt = "tamanho do item "+items.size();
					for (FileItem item : items) {
						if (item.isFormField()) {
							txt = item.getFieldName();
							txt = "teste4";
							txt = item.getString();
						} else {
							txt = "teste5";
							txt = item.getFieldName();
							HSSFWorkbook workbook = new HSSFWorkbook(item.getInputStream());
							HSSFSheet sheet = (HSSFSheet) workbook.getSheetAt(0);
							Row row;
							for (int i = 1; i <= sheet.getLastRowNum(); i++) { 
								row = (Row) sheet.getRow(i); // sheet number

								if (row.getCell(0) == null) {
									// String name = "0";
								} // suppose excel cell is empty then its set to 0 the variable
								else {
									request.setAttribute("txtefile", row.getCell(0).toString());
								} // else copies cell data to name variable

								// if( row.getCell(1)==null) {String age = "0"; }
								// else age= row.getCell(1).toString();

								// if( row.getCell(2)==null) {String address = "0"; }
								// else address = row.getCell(2).toString();
							}
						}
					}

				} catch (FileUploadException e) {
					e.getMessage();
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
		return new ModelAndView(this.getSuccessView(), map);
	}
	
	@Override
	protected void onFormChange(HttpServletRequest request, HttpServletResponse response, Object command) throws Exception {
		
	}
}
