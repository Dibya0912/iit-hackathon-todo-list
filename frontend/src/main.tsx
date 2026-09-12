import React from 'react'
import ReactDOM from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router-dom'
import { MotionConfig } from 'framer-motion'
import App from './App'
import './styles.css'
import './readability.css'
const client=new QueryClient({ defaultOptions:{ queries:{ retry: (count,error)=> !('status' in error && [401,403].includes(Number(error.status))) && count<1, refetchOnWindowFocus:true, staleTime:5000 }, mutations:{retry:false} } })
ReactDOM.createRoot(document.getElementById('root')!).render(<React.StrictMode><QueryClientProvider client={client}><BrowserRouter><MotionConfig reducedMotion="user"><App/></MotionConfig></BrowserRouter></QueryClientProvider></React.StrictMode>)
