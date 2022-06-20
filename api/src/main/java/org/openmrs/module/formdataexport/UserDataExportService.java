package org.openmrs.module.formdataexport;

import java.util.List;

import org.openmrs.User;
import org.openmrs.api.OpenmrsService;
import org.openmrs.api.db.LoginCredential;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface UserDataExportService extends OpenmrsService {
	public List<User> getUserByUsername(String username);
	public LoginCredential getUserLoginCredential(User user);
}
