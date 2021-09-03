import React from 'react'

export default function IconLabel({icon, label}) {
  return (<span style={{display: 'inline-flex', alignItems: 'center'}}>
    {icon}&nbsp;{label}
  </span>)
}