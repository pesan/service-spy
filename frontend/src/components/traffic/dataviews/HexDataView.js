import React from 'react';
import PreFormattedDataView from './PreFormattedDataView'

export default function HexView({data}) {

  let result = ''
  for (let i = 0; i < data.length; i += 16) {
    let hexColumn = '', charColumn = ''
    for (let c = i; c < i + 16; c++) {
      if (c < data.length) {
        const ch = data.charCodeAt(c)
        hexColumn += ch.toString(16).padStart(2, '0') + (c - i === 7 ? '   ' : ' ')
        charColumn += (ch >= 32 ? data.charAt(c) : '.') + (c - i === 7 ? ' ' : '')
      } else {
        hexColumn += "   " + (c - i === 7 ? '   ' : '')
        charColumn += " " + (c - i === 7 ? ' ' : '')
      }
    }
    result += i.toString(16).padStart(8, '0') + '  ' + hexColumn + ' ' + charColumn + '\n'
  }
  return <PreFormattedDataView>{result}</PreFormattedDataView>;
}
