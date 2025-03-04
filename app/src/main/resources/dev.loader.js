const moduleAlias = require('module-alias')

// see:
//  https://www.npmjs.com/package/module-alias
//
moduleAlias.addAliases({
  'react-redux': 'react-redux/lib/alternate-renderers'
})

const {FarjsApp} = require("./farjs-app-fastopt")

const showDevTools = true
FarjsApp.start(showDevTools)
