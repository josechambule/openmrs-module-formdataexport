package org.openmrs.module.formdataexport.model;

import org.springframework.web.multipart.MultipartFile;

public class FileUpload {
	
	private MultipartFile file;

	  public void setFile(MultipartFile file) {
	    this.file = file;
	  }

	  public MultipartFile getFile() {
	    return file;
	  }

	public FileUpload() {
		// TODO Auto-generated constructor stub
	}

}
