package org.openmrs.module.formdataexport;

import java.util.List;

import org.openmrs.User;
import org.openmrs.api.OpenmrsService;
import org.openmrs.api.db.LoginCredential;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author jose.chambule
 *
 */
@Transactional
public interface UserDataExportService extends OpenmrsService {
	public List<User> getUserByUsername(String username);
	public LoginCredential getUserLoginCredential(User user);
	public void importUser(User user, LoginCredential credentials);
}
