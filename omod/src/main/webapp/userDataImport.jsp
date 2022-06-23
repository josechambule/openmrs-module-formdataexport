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
	function UploadProcess() {
		var fileUpload = document.getElementById("filename");
		var regex = /^([a-zA-Z0-9\s_\\.\-:])+(.xls|.xlsx)$/;
        if (regex.test(fileUpload.value.toLowerCase())) {
        	
        	//alert('The file "' + fileUpload.value +  '" has been selected.');
        	//window.location.assign("userDataExport.list?txtefile=" + fileUpload.value);
        	document.getElementById("txts").value = fileUpload.value;
        }
	}
</script>
<b class="boxHeader">Import Users</b>
<div class="box">
	<form method="post" enctype="multipart/form-data">
		<spring:message code="formdataexport.UserDataImport.file" />
		: <input type="file" name="filename" id="filename" accept=".xls,.xlsx" onchange="UploadProcess()"/>
		<input type="submit" 
			value="<spring:message code="formdataexport.UserDataImport.import"/>" /><br />${txtefile}
			<input type="text" name="path" id="txts"/>
	</form>
</div>
<%@ include file="/WEB-INF/template/footer.jsp"%>