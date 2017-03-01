angular
        .module("app", [])
        .controller(
                "owner",
                function($http, $sce) {
                    var self = this;
                    self.pets = [];
                    fetch = function(pet) {
                        $http
                                .get("videos/" + pet.id)
                                .then(
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
                    self.rate = function(pet, stars) {
                        $http
                                .post("videos/" + pet.id, {
                                    stars : stars
                                })
                                .then(
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
                    $http.get("#").then(function(response) {
                        self.pets = response.data.pets;
                        for (var i = 0; i < self.pets.length; i++) {
                            var pet = self.pets[i]
                            fetch(pet);
                        }
                    });
                });
