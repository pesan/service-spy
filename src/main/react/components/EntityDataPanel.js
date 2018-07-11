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
import Grid from "@material-ui/core/Grid";

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
  return <CodeArea>{result}</CodeArea>;
}

function CodeArea({children, style}) {
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

class EntityDataPanel extends Component {
  state = {
    tabIndex: 0,
    renderer: 'None'
  }

  renderers = [
    { name: 'XML', mimeType: ['application', 'xml'], renderer: text => <CodeArea>{formatters.xml(text)}</CodeArea> },
    { name: 'JSON', mimeType: ['application', 'json'], renderer: text => <CodeArea>{formatters.json(text)}</CodeArea> },
    { name: 'CSS', mimeType: ['text, css'], renderer: text => <CodeArea>{formatters.css(text)}</CodeArea> },
    { name: 'Text', mimeType: ['text', ''], renderer: text => <CodeArea>{text}</CodeArea>},
    { name: 'Image (256x256)', mimeType: ['image', ''], renderer: (_, href) => <img src={href} width="256" height="256" alt="Rendered data representation"/>},
    { name: 'Image (full)', mimeType: ['image', ''], renderer: (_, href) => <img src={href} alt="Rendered data representation"/>},
    { name: 'Binary', mimeType: ['', ''], renderer: data => <HexView data={data}/>},
    { name: 'None', mimeType: ['', ''], renderer: _ => <span/>},
  ]

  renderHeaders = (headers) => {
    if (!headers) {
      return <Typography>No headers</Typography>
    }
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
            <TableCell><code>{key}</code></TableCell>
            <TableCell>{headers[key].map((value, index) => <span key={`header-${key}-${index}`}><code>{value}</code> </span>)}</TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>)
  }

  renderException = exception => {
    return <CodeArea style={{color: 'red'}}>{exception.name}: {exception.message}{"\n"}
      {exception.stackTrace.map(traceItem => {
        const lineNumber = traceItem.nativeMethod ? '<native>' : traceItem.lineNumber;
        return `  at ${traceItem.className}.${traceItem.methodName}(${traceItem.fileName}:${lineNumber})`
      }).join("\n")}</CodeArea>
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

  componentDidMount(props) {
    this.setState({renderer: this.getDataRenderer(this.props.item.contentType).name})
  }

  render() {
    const {id, kind, href, item} = this.props
    const dataRenderer = this.renderers.find(renderer => renderer.name === this.state.renderer)
    return (<Grid style={{overflow: 'auto'}} item xs={6}>
      <Tabs value={this.state.tabIndex}>
        <Tab label={kind + ' Data'} onClick={() => this.setState({tabIndex: 0})}/>
        <Tab label={kind + ' Headers (' + Object.keys(item.headers || {}).length + ')'}
             onClick={() => this.setState({tabIndex: 1})}/>
      </Tabs>

      {this.state.tabIndex === 0
        ?
        item.exception
          ? (<div>
            <Typography variant="subheading">{kind} exception details:</Typography>
            {this.renderException(item.exception)}
          </div>)
          :
          (<div>
            <FormControl>
              <InputLabel/>
              <Select value={this.state.renderer} onChange={e => this.setState({renderer: e.target.value})}>
                {this.renderers.map(renderer =>
                  <MenuItem value={renderer.name} key={renderer.name}>
                    {renderer.name}
                  </MenuItem>
                )}
              </Select>
            </FormControl>
            {href ?
              <IconButton href={href} download={id + '-' + kind} title="Download"><FileDownloadIcon/></IconButton> :
              <span/>}
            <br/>
            {item.data ? dataRenderer.renderer(item.data, href) : 'Empty body'}
          </div>)
        : this.renderHeaders(item.headers)
      }
    </Grid>)
  }
}

export default EntityDataPanel;