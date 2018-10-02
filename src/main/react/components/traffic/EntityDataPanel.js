import React, {Component} from "react";
import formatters from "common/Formatters"

import Typography from "@material-ui/core/Typography";
import IconButton from "@material-ui/core/IconButton";
import Table from "@material-ui/core/Table";
import TableHead from "@material-ui/core/TableHead";
import FileDownloadIcon from "@material-ui/icons/FileDownload";
import TableRow from "@material-ui/core/TableRow";
import TableCell from "@material-ui/core/TableCell";
import TableBody from '@material-ui/core/TableBody';
import Tabs from '@material-ui/core/Tabs';
import Tab from '@material-ui/core/Tab';

import Select from '@material-ui/core/Select'
import FormControl from '@material-ui/core/FormControl'
import InputLabel from '@material-ui/core/InputLabel'
import MenuItem from '@material-ui/core/MenuItem'

function HexView({data}) {
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
  return <Preformatted>{result}</Preformatted>;
}

function Preformatted({children, style}) {
  return <pre style={{
    maxHeight: 320,
    border: '1px solid lightgrey',
    borderRadius: '6px',
    overflow: 'auto',
    margin: '.2em',
    padding: '.5em 0 .5em .5em',
    ...style,
  }}>{children}</pre>
}

function Exception({exception}) {
  const stackTraceItem = traceItem => {
    const lineNumber = traceItem.nativeMethod ? '<native>' : traceItem.lineNumber;
    return `  at ${traceItem.className}.${traceItem.methodName}(${traceItem.fileName}:${lineNumber})`
  }

  return <Preformatted style={{color: 'red'}}>
    {exception.name}: {exception.message}{"\n"}
    {exception.stackTrace.map(stackTraceItem).join("\n")}
  </Preformatted>
}

function EntityHeadersTable({headers}) {
  return (<Table>
    <TableHead>
      <TableRow>
        <TableCell>Key</TableCell>
        <TableCell>Values</TableCell>
      </TableRow>
    </TableHead>
    <TableBody>
      {Object.keys(headers).map(key => (
        <TableRow key={key}>
          <TableCell>{key}</TableCell>
          <TableCell>{headers[key].map((value, index) => <span key={`header-${key}-${index}`}><code>{value}</code></span>)}</TableCell>
        </TableRow>
      ))}
    </TableBody>
  </Table>)
}

class EntityDataPanel extends Component {
  state = {
    tabIndex: 0,
    renderer: 'None'
  }

  renderers = [
    { name: 'XML', mimeType: ['application', 'xml'], renderer: text => <Preformatted>{formatters.xml(text)}</Preformatted> },
    { name: 'JSON', mimeType: ['application', 'json'], renderer: text => <Preformatted>{formatters.json(text)}</Preformatted> },
    { name: 'CSS', mimeType: ['text, css'], renderer: text => <Preformatted>{formatters.css(text)}</Preformatted> },
    { name: 'Text', mimeType: ['text', ''], renderer: text => <Preformatted>{text}</Preformatted>},
    { name: 'Image (256x256)', mimeType: ['image', ''], renderer: (_, href) => <img src={href} width="256" height="256" alt="Rendered data representation"/>},
    { name: 'Image (actual size)', mimeType: ['image', ''], renderer: (_, href) => <img src={href} alt="Rendered data representation"/>},
    { name: 'Binary', mimeType: ['', ''], renderer: data => <HexView data={data}/>},
    { name: 'None', mimeType: ['', ''], renderer: _ => <span/>},
  ]

  renderHeaders = (headers) => {
    return headers
      ? <EntityHeadersTable headers={headers}/>
      : <Typography>No headers</Typography>
  }

  renderBody = (id, kind, href, item, dataRenderer) => {
    return item.exception
      ? (<div>
        <Typography variant="subheading">{kind} exception details:</Typography>
        <Exception exception={item.exception}/>
      </div>)
      : item.data
        ?

        <div>
          <FormControl style={{minWidth: '10rem'}}>
            <InputLabel htmlFor={`${kind}-dataformat-${id}`}>Display as</InputLabel>
            <Select id={`${kind}-dataformat-${id}`} label="Label" value={this.state.renderer}
                    onChange={e => this.setState({renderer: e.target.value})}>
              {this.renderers.map(renderer =>
                <MenuItem value={renderer.name} key={renderer.name}>
                  {renderer.name}
                </MenuItem>
              )}
            </Select>
          </FormControl>
          {href &&
          <IconButton href={href} download={id + '-' + kind} title="Download"><FileDownloadIcon/></IconButton>}
          {dataRenderer.renderer(item.data, href)}
        </div>
        : 'Empty body'
  }

  getDataRenderer(contentType) {
    const [type, subtype] = (contentType||'/').split('/')

    for (let renderer of this.renderers) {
      if (type.indexOf(renderer.mimeType[0]) >= 0 && subtype.indexOf(renderer.mimeType[1]) >= 0) {
        return renderer
      }
    }

    throw new Error("could not render content of type: " + contentType)
  }

  componentDidMount() {
    this.setState({renderer: this.getDataRenderer(this.props.item.contentType).name})
  }

  render() {
    const {id, kind, href, item} = this.props
    const dataRenderer = this.renderers.find(renderer => renderer.name === this.state.renderer)
    const tabs = [
      {
        label: `${kind} Data`, render: () => {
          return this.renderBody(id, kind, href, item, dataRenderer)
        }
      },
      {
        label: `${kind} Headers (${Object.keys(item.headers || {}).length})`,
        render: () => this.renderHeaders(item.headers)
      },
    ]

    return (<React.Fragment>
      <Tabs value={this.state.tabIndex}>
        {tabs.map((tab, index) =>
          <Tab label={tab.label} onClick={() => this.setState({tabIndex: index})}/>
        )}
      </Tabs>

      {tabs[this.state.tabIndex].render()}
    </React.Fragment>)
  }
}

export default EntityDataPanel;