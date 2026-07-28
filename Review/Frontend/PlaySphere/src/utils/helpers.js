export const fmtDate=(date)=>{

return new Date(date)
.toLocaleString(
"en-IN",
{
dateStyle:"medium",
timeStyle:"short"
}
);

};



export const demoPayment = async(paymentId,post)=>{

await post(
"/payments/verify",
{
paymentId,
razorpayPaymentId:`demo_pay_${Date.now()}`
}
);


};



export const normalizeTime=(time)=>{

return time.replace(/\./g,":");

};



export const createDateWithIST=(date,time)=>{


const normalized =
normalizeTime(time);


return new Date(
`${date}T${normalized}:00+05:30`
);


};