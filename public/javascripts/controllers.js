var snipStoryControllers = angular.module('snipStoryControllers', ['ui.bootstrap', 'snipStoryServices']);

snipStoryControllers.controller('EditorCtrl', ['$scope', '$http', '$modal', 'imageHandler',
  function ($scope, $http, $modal, imageHandler) {
	$http.get('/mystory/all').success(function(data) {
	  $scope.story = data;
	  $scope.curChapter = data.chapters[0];
	  $scope.pageIdx = 0; //TODO: update with scroll
	});
	 
    $scope.orderProp = 'age';
    
    $scope.getPicUrl = function getPicUrl(picture, thumbType) {
    	var url = picture.url;
    	var index = url.indexOf(picture.id) + picture.id.length + 1;
    	return url.substring(0,index) + "thumb_" + thumbType + "." + url.substring(index, url.length);
    };
    
    $scope.showAddSnippetDialog = function() {
    	
    	var addSnippetModal = $modal.open({
    		templateUrl: 'addSnippetDialog.html',
    		controller: 'AddSnippetDialogCtrl',
    	});
    	
    	addSnippetModal.result.then(function (result) {   		
    		//create item
    		var ORDER_INTERVAL = 1000000;
    		var pageIdx = $scope.pageIdx;
    		var page = $scope.curChapter.pages[pageIdx];
    		var items = page.items;
    		var ordering = ORDER_INTERVAL;
    		if (items.length > 0)
    			ordering += items[items.length - 1].ordering;
    		var newItem = {"description":result.caption, "type":"IMAGE", "ordering":ordering};
    		$http.post('/pages/' + page.id + '/item', newItem, {})
			.success(function(data, status, headers, config) {
				$scope.curChapter.pages[pageIdx].items.push(data);
				//TODO: modify item with image once it is generated
				
			}).error(function(data, status, headers, config) {
				//TODO: add error handler of some sort
			});
    		
    	});
    	
    };
    
}]);

snipStoryControllers.controller('AddSnippetDialogCtrl', ['$scope', '$modalInstance', 'imageHandler',
	function($scope, $modalInstance, imageHandler) {
	
	$scope.okToAdd = false;
	$scope.hasSelectedPic = false;
	$scope.thumbnail = null;
	$scope.picId = null;
	$scope.caption = null;
	
	$scope.selectPic = function() {
		$("#picFileInput").click();
	};
	
	$scope.uploadPic = function uploadPic(picFile) {
		var picId = imageHandler.doThumbnailAndUpload(picFile, function() {
			$scope.thumbnail = imageHandler.pics[picId.toString()].original;
			$scope.$apply();
		});
		$scope.picId = picId;
	};
	
	$scope.ok = function() {
		$modalInstance.close({picId:$scope.picId, caption:$scope.caption});
	};
	$scope.cancel = function() {
		//TODO: if picture is uploaded, delete it - if it is queued for upload, queue if for deletion
		$modalInstance.dismiss('cancel');
	};
}]);