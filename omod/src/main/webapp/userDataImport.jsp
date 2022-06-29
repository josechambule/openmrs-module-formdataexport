<%@ include file="/WEB-INF/template/include.jsp"%>

<%@ include file="/WEB-INF/template/header.jsp"%>

<openmrs:htmlInclude file="/scripts/calendar/calendar.js" />
<openmrs:htmlInclude file="/scripts/jquery/jquery-1.3.2.min.js" />
<script type="text/javascript">
	var $j = jQuery.noConflict();
</script>
<h2>
	<spring:message code="formdataexport.UserDataImport.title" />
</h2>
<script type="text/javascript">
// async function UploadProcess() {
// 		var fileUpload = document.getElementById("file");
// 		var regex = /^([a-zA-Z0-9\s_\\.\-:])+(.xls|.xlsx)$/;
//         if (regex.test(fileUpload.value.toLowerCase())) {
        	
//         	alert('The file "' + fileUpload.value +  '" has been selected.');
//         	window.location.assign("userDataExport.list?txtefile=" + fileUpload.value);
//         	document.getElementById("txts").value = fileUpload.files.item(0).size + " " +fileUpload.files.item(0).name;
//         	var file = fileUpload.files.item(0);
        	
//         }
// 	}
</script>
<b class="boxHeader">Import Users</b>
<div class="box">
	<form method="post" action="userDataImport.form" enctype="multipart/form-data">
		<spring:message code="formdataexport.UserDataImport.file" />
		: <input type="file" name="file" id="file" accept=".xls,.xlsx"/>
		<input type="submit" 
			value="<spring:message code="formdataexport.UserDataImport.import"/>" /><br />${txtfile}
	</form>
</div>
<%@ include file="/WEB-INF/template/footer.jsp"%>