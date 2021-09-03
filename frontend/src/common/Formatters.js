import vkbeautify from 'vkbeautify'

const createFormatter = (formatFunction) => (text, defaultValue = text) => {
  try {
    return formatFunction(text, 2)
  } catch (e) {
    return defaultValue
  }
}


export default {
  json: createFormatter(vkbeautify.json),
  xml: createFormatter(vkbeautify.xml),
  css: createFormatter(vkbeautify.css),
}