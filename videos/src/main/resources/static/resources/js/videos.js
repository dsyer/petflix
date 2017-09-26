angular.module("page", []).controller(
    "videos",
    function($scope, $http, $sce) {
      var pet = $scope.pet;
      var self = this;
      fetch = function() {
        $http.get("/videos/" + pet.id).then(
            function(response) {
              var stars = response.data.stars;
              pet.stars = {};
              for (var i = 0; i < stars; i++) {
                pet.stars[i] = "star";
              }
              for (var i = stars; i < 5; i++) {
                pet.stars[i] = "star-empty";
              }
              pet.video = $sce
                  .trustAsHtml('<iframe width="500" height="300" src="'
                      + response.data.url
                      + '" frameborder="0" allowfullscreen></iframe>');
            });
      };
      self.rate = function(stars) {
        $http.post("/commands/rate-video", {
          pet : pet.id,
          stars : stars
        }).then(function(response) {
          var stars = response.data.stars;
          pet.stars = {};
          for (var i = 0; i < stars; i++) {
            pet.stars[i] = "star";
          }
          for (var i = stars; i < 5; i++) {
            pet.stars[i] = "star-empty";
          }
        });
      };
      fetch(pet);
    });
