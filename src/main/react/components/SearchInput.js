import React from 'react'
import Input from "@material-ui/core/Input";
import InputAdornment from "@material-ui/core/InputAdornment";
import IconButton from "@material-ui/core/IconButton";
import CloseIcon from "@material-ui/icons/Close"
import SearchIcon from "@material-ui/icons/Search"

export default function SearchInput({value, onChange, placeholder}) {
  return <Input
    placeholder={placeholder ||'Filter...'}
    value={value}
    onChange={e => onChange(e.target.value)}
    startAdornment={<InputAdornment position="start"><SearchIcon/></InputAdornment>}
    endAdornment={<InputAdornment position="end">
      <IconButton onClick={() => onChange('')}>
        <CloseIcon style={{fontSize: 14}}/>
      </IconButton>
    </InputAdornment>}
  />
}