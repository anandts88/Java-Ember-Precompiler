this.Handlebars = Handlebars;

var Ember = {
	assert: function() {}
};

function precompile(arg) {
  return Ember.Handlebars.precompile(arg).toString().replace('function anonymous', 'function');
}
