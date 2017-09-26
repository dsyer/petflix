angular.module("app", ["page"]).controller("owner", function($http, $sce) {
  var self = this;
  self.pets = [];
  $http.get("#").then(function(response) {
    self.pets = response.data.pets;
  });
});
