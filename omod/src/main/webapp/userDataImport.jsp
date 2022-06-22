<%@ include file="/WEB-INF/template/include.jsp"%>
<openmrs:require privilege="Manage Data Exports" otherwise="/login.htm"
	redirect="/module/formdataexport/userDataImport.form" />
<%@ include file="/WEB-INF/template/header.jsp"%>
<%@ include file="localHeader.jsp"%>
<openmrs:htmlInclude file="/scripts/calendar/calendar.js" />
<openmrs:htmlInclude file="/scripts/jquery/jquery-1.3.2.min.js" />
<script type="text/javascript">
	var $j = jQuery.noConflict();
</script>
<h2>
	<spring:message code="formdataexport.UserDataImport.title" />
</h2>
<script type="text/javascript">
	function importData() {
		var filename = document.getElementById("filename").addEventListener(
				'change', handleFileSelect, false);
	}

	function handleFileSelect(evt) {

		var files = evt.target.files; // FileList object

	}
</script>
<b class="boxHeader">Import Users</b>
<div class="box">
	<form method="post" enctype="multipart/form-data">
		<spring:message code="formdataexport.UserDataImport.file" />
		: <input type="file" name="filename" id="filename" value="${txtfile}"/> 
		<input type="submit"
			value="<spring:message code="formdataexport.UserDataImport.import"/>" /><br />${txtefile}
	</form>
</div>
<%@ include file="/WEB-INF/template/footer.jsp"%>